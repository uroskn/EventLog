#include <stdio.h>
#include <stdlib.h>
#include <sqlite3.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>
#include <sys/file.h>
#include <time.h>
#include "pipeproc.h"

#define MAX_EXPECTED_DATA 65535
#define MAX_ENTRIES       128

char** entries[MAX_ENTRIES];
int    current_entry_count = 0;

int passthru_fd = -1;

volatile int do_flush = 0;
volatile int triggered_alarm = 0;
volatile int keep_locked = 0;

const char* database;

int sync_file = -1;

void open_passthru()
{
  if (passthru_fd == -1)
  {
    mkfifo(OUTPUT_PIPE, 0644);
    passthru_fd = open(OUTPUT_PIPE, O_WRONLY | O_NONBLOCK);
    if (passthru_fd != -1)
    {
      fcntl(passthru_fd, F_SETFL, (fcntl(passthru_fd, F_GETFL, 0) & ~O_NONBLOCK));
    }
  }
}

void lock_sync_file()
{
  alarm(0);
  if (sync_file == -1)
  {
    sync_file = open(SYNC_FILE, O_RDWR | O_CREAT);
    flock(sync_file, LOCK_EX);
  }
}

void unlock_sync_file()
{
  if (sync_file != -1)
  {
    flock(sync_file, LOCK_UN);
    close(sync_file);
    sync_file = -1;
  }
}

void flush_entries()
{
  do_flush = 0;
  triggered_alarm = 0;
  sqlite3* db;
  int fd = open(database, O_RDWR);
  if (fd == -1) { perror("Can't open file, WTF?\n"); exit(1); }
  flock(fd, LOCK_EX);
  int rc = sqlite3_open(database, &db);
  if (rc) { printf("SQLITE error, can't open, WTF? %d\n", rc); exit(1); }
  int i;
  sqlite3_exec(db, "PRAGMA synchronous = OFF", NULL, NULL, NULL);
  sqlite3_exec(db, "PRAGMA journal_mode = MEMORY", NULL, NULL, NULL);
  sqlite3_exec(db, "BEGIN TRANSACTION", NULL, NULL, NULL);
  sqlite3_stmt* stmt;
  sqlite3_prepare(db, "INSERT INTO events (eventstr) VALUES (?)", -1, &stmt, NULL);
  for (i = 0; i < current_entry_count; i++)
  {
    sqlite3_bind_blob(stmt, 1, entries[i], strlen((const char*)entries[i]), SQLITE_TRANSIENT);
    while (sqlite3_step(stmt) != SQLITE_DONE) usleep(100);
    sqlite3_reset(stmt);
    free(entries[i]);
  }
  current_entry_count = 0;
  sqlite3_exec(db, "END TRANSACTION", NULL, NULL, NULL);
  sqlite3_finalize(stmt);
  sqlite3_close(db);
  flock(fd, LOCK_UN);
  close(fd);
  open_passthru();
}

void signal_handler(int signo) { do_flush = 1; triggered_alarm = 0; keep_locked = 0; }

struct timespec time_diff(struct timespec start, struct timespec end)
{
  struct timespec temp;
  if ((end.tv_nsec - start.tv_nsec) < 0)
  {
    temp.tv_sec = end.tv_sec + (start.tv_sec - 1);
    temp.tv_nsec = 1000000000 + end.tv_nsec - start.tv_nsec;
  }
  else
  {
    temp.tv_sec = end.tv_sec - start.tv_sec;
    temp.tv_nsec = end.tv_nsec - start.tv_nsec;
  }
  return temp;
}

int main(int argc, char** argv)
{
  if (argc < 2)
  {
    printf("Pot do baze?\n");
    return 0;
  }
  signal(SIGPIPE, SIG_IGN);
  open_passthru();
  sqlite3* db;
  database = (const char*)argv[1];
  int rc = sqlite3_open(database, &db);
  if (rc)
  {
    printf("WTF, Sqlite error %d\n", rc);
    return 1;
  }
  sqlite3_exec(db, "CREATE TABLE events (" \
                   "  eventno INTEGER PRIMARY KEY AUTOINCREMENT," \
                   "  eventstr BLOB" \
                   ");", NULL, NULL, NULL);
  sqlite3_close(db);
  struct sigaction sa;
  sa.sa_handler = signal_handler;
  sa.sa_flags = SA_RESTART;
  sigemptyset(&sa.sa_mask);
  sigaction(SIGALRM, &sa, NULL);
  struct timespec start;
  clock_gettime(CLOCK_MONOTONIC, &start);
  while (!feof(stdin))
  {
    char* data = (char*)malloc(sizeof(char*)*MAX_EXPECTED_DATA);
    char* result = fgets(data, MAX_EXPECTED_DATA, stdin);
    if (result != NULL)
    {
      if ((passthru_fd != -1) && (write(passthru_fd, result, strlen(result)) == -1))
      {
        close(passthru_fd);
        passthru_fd = -1;
      }
      entries[current_entry_count++] = result;
      if (current_entry_count == MAX_ENTRIES)
      {
        do_flush = 1;
        struct timespec end;
        clock_gettime(CLOCK_MONOTONIC, &end);
        struct timespec total = time_diff(start, end);
        if ((total.tv_sec > 1) || (total.tv_nsec > 10000000)) keep_locked = 0;
        else keep_locked++;
        if (keep_locked > 3) lock_sync_file();
      }
      if ((!triggered_alarm) && (!do_flush))
      {
        triggered_alarm = 1;
        alarm(1);
      }
    }
    if (do_flush)
    {
      lock_sync_file();
      flush_entries();
      if (!keep_locked) unlock_sync_file();
      clock_gettime(CLOCK_MONOTONIC, &start);
    }
  }
  lock_sync_file();
  flush_entries();
  unlock_sync_file();
}

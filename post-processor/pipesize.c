#define _GNU_SOURCE
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <error.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
  if (argc <= 3)
  {
    printf("Usage: %s <fd number> <pipe size> <executable>\n", argv[0]);
    return 0;
  }
  char* parmlist[3];
  parmlist[0] = "/bin/sh";
  parmlist[1] = "-c";
  parmlist[2] = (char*)calloc(sizeof(char), (strlen("exec ")+strlen(argv[3])+1));
  strcat(parmlist[2], "exec ");
  strcat(parmlist[2], argv[3]);
  if (fcntl(atoi(argv[1]), F_SETPIPE_SZ, atoi(argv[2])) == -1) 
  {
    perror("Can't set pipe size");
    return 1;
  }
  execv("/bin/sh", parmlist); 
  return 2;
}

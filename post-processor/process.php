#!/usr/bin/php
<?php

  //define("EXPECT_JSON", 1);

  define("READ_FILE",   "/tmp/events.queue.sqlite");
  define("BASE_PATH",   "/srv/mcserver/eventlog/");
  define("_DEFAULT",    BASE_PATH."other/%DATE%.log.xz");
  define("COMMAND",     "nice -n 19 ./pipesize 0 1024000 'xz -cef0'");
  //define("COMMAND",     "cat");
  define("SYNC_FILE",   "/tmp/eventlog.sync");

  function IsCreateRec($data) {
    foreach ($data as $k => $v) {
      if (is_array($v)) { if (IsCreateRec($v)) return true; }
      if (($k == "gamemode") && ($v == "CREATIVE")) return true;
      if (($k == "gamemode") && ($v == "SPECTATOR")) return true;
    }
    return false;
  }

  $filter = function($obj) {
    $event = $obj["event"];
    return $obj;
  };

  $sort = array(
    BASE_PATH."liquid-flow/%DATE%.log.xz" => array("ELLiquidFlow"),

    BASE_PATH."chunks/%DATE%.log.xz" => array("ChunkLoadEvent", "ChunkUnloadEvent", "ChunkPopulateEvent"),
    BASE_PATH."admin/%DATE%.log.xz" => function ($data) { return IsCreateRec($data); },

    BASE_PATH."items/hopper/%DATE%.log.xz" => array("InventoryMoveItemEvent"),
    BASE_PATH."items/other/%DATE%.log.xz"  => array(
      "ItemSpawnEvent", "ItemDespawnEvent", "PlayerPickupItemEvent", "PlayerDropItemEvent",
      "PlayerItemBreakEvent", "PlayerItemConsumeEvent", "ItemMergeEvent",
      "PlayerItemDamageEvent", "PlayerPickupArrowEvent",
      function ($data) { 
        return (($data["event"] == "ELEntityRemoved") && 
                (@$data["entity"]["entity"]["type"] == "DROPPED_ITEM")); 
      }
    ),
    BASE_PATH."blocks/%DATE%.log.xz"      => array(
      "PlayerBucketFillEvent", "PlayerBucketEmptyEvent", "BlockFormEvent",
      "BlockBurnEvent", "BlockIgniteEvent", "BlockFadeEvent",
      "BlockGrowEvent", "EntityChangeBlockEvent", "BlockSpreadEvent", 
      "BlockPlaceEvent", "BlockBreakEvent", "BlockFromToEvent",
      "EntityCreatePortalEvent", "HangingBreakByEntityEvent", "EntityExplodeEvent", 
      "PortalCreateEvent", "StructureGrowEvent",
      "ExplosionPrimeEvent", "LeavesDecayEvent", "SignChangeEvent", "BlockExpEvent",
      "BlockExplodeEvent", "BlockCanBuildEvent", "CauldronLevelChangeEvent",
      "BrewingStandFuelEvent", "BlockPistonExtendEvent", "BlockPistonRetractEvent",
      "BlockMultiPlaceEvent",
    ),

    BASE_PATH."player/movement/%DATE%.log.xz"    => array(
      "PlayerMoveEvent", "VehicleMoveEvent", "PlayerToggleSprintEvent", 
      "PlayerToggleSneakEvent", "PlayerToggleFlightEvent",
      "PlayerPortalEvent",
    ),
    BASE_PATH."player/interact/%DATE%.log.xz"    => array(
      "PlayerInteractEvent", "PlayerInteractEntityEvent"
    ),
    BASE_PATH."player/health/%DATE%.log.xz"      => array(
      function ($data) { 
        if (!isset($data["entity"]["isplayer"])) 
          throw new Exception("STOP THIS MADNESS!"); 
      },
      "EntityDamageEvent", "EntityDamageByEntityEvent", "EntityDamageByBlockEvent", 
      "EntityRegainHealthEvent", "EntityCombustByBlockEvent",
      "EntityCombustByEntityEvent", "PlayerDeathEvent",
    ),
    BASE_PATH."player/inventory/%DATE%.log.xz"   => array(
      "PlayerItemHeldEvent", "InventoryOpenEvent", "InventoryCreativeEvent",
      "InventoryCloseEvent", "CraftItemEvent", "FurnaceExtractEvent",
      "ELContainerTx", "PlayerSwapHandItemsEvent", "PrepareItemEnchantEvent", 
      "PlayerArmorStandManipulateEvent",
    ),
    BASE_PATH."player/chat/%DATE%.log.xz"        => array(
      function ($data) { return (($data["event"] == "PlayerCommandPreprocessEvent") &&
                                 (substr($data["message"], 0, 9) != "/register") &&
                                 (substr($data["message"], 0, 7) != "/login") &&
                                 (substr($data["message"], 0, 9) != "/changepw")); },
      "AsyncPlayerChatEvent"
    ),
    BASE_PATH."player/other/%DATE%.log.xz"       => array(
      "PlayerKickEvent", "PlayerGameModeChangeEvent", "PlayerBedEnterEvent", 
      "PlayerBedLeaveEvent", "PlayerFishEvent", "PlayerRespawnEvent",
      "PlayerJoinEvent", "PlayerQuitEvent", "PlayerLoginEvent", 
      "PlayerChangedWorldEvent", "PlayerEggThrowEvent", "PlayerTeleportEvent",
      "PlayerLevelChangeEvent", "PlayerVelocityEvent", "PlayerExpChangeEvent", 
      "FoodLevelChangeEvent", "PlayerEditBookEvent", "PlayerShearEntityEvent", 
      "EnchantItemEvent", "BlockDamageEvent", "HangingPlaceEvent", 
      "PlayerAchievementAwardedEvent", "PlayerInteractAtEntityEvent",
      "PlayerUnleashEntityEvent", "PlayerLeashEntityEvent",
      function ($data) { 
        return (($data["event"] == "VehicleExitEvent") && 
                (isset($data["exited"]["isplayer"]))); 
      },
      function ($data) { 
        return (($data["event"] == "VehicleEnterEvent") && 
                (isset($data["entered"]["isplayer"]))); 
      },
      function ($data) { 
        return (($data["event"] == "VehicleDestroyEvent")  && 
                (isset($data["attacker"]["isplayer"]))); 
      },
      function ($data) { 
        $parray = array("EntityToggleGlideEvent", "EntityAirChangeEvent",
                        "EntityResurrectEvent");
        return ((in_array($data["event"], $parray)) && 
                (isset($data["entity"]["isplayer"])));
      },
    ),

    BASE_PATH."entity/targeting/%DATE%.log.xz" => array(
      "EntityTargetLivingEntityEvent", "EntityTargetEvent", 
      "ELPigmanCoolDownEvent", "ELPigmanAngerEvent"
    ),
    BASE_PATH."entity/spawn/%DATE%.log.xz"     => array(
      "EntityDeathEvent", "CreatureSpawnEvent", "ELEntityRemoved", 
      "SpawnerSpawnEvent"
    ),
    BASE_PATH."entity/health/%DATE%.log.xz"    => array(
      function ($data) { if (isset($data["entity"]["isplayer"])) throw new Exception("STOP THIS MADNESS!"); },
      "EntityDamageEvent", "EntityDamageByEntityEvent", "EntityDamageByBlockEvent", 
      "EntityRegainHealthEvent", "EntityCombustByBlockEvent", "EntityCombustByEntityEvent",
      "EntityAirChangeEvent", "EntityResurrectEvent",
    ),
    BASE_PATH."entity/other/%DATE%.log.xz"     => array(
      "EntityTameEvent", "EntityBreakDoorEvent", "EntityPortalEvent", "EntityShootBowEvent",
      "EntityPortalExitEvent", "EntityInteractEvent", "EntityPortalEnterEvent", 
      "EntityTeleportEvent", "EntityBreedEvent", "EnderDragonChangePhaseEvent",
      "VehicleExitEvent", "VehicleEnterEvent", "VehicleDestroyEvent",
      "EntityToggleGlideEvent", "FireworkExplodeEvent",
    ),
  );

  function _SimpleDecode($string, &$i, $level = 0)
  {
    $data       = array();
    $objname    = "";
    $objpayload = "";
    $instr      = false;
    $progress   = 0;
    do
    {
      $i++;
      if (!$instr)
      {
        if ($string[$i] == "}") return $data;
        else if (($string[$i] == '"') && ($progress == 1)) $instr = true;
        else if ($string[$i] == ":") $progress = 1;
        else if ($string[$i] == '{') $objpayload = _SimpleDecode($string, $i, ($level +1));
        else if ($string[$i] == ",")
        {
          $data[$objname] = $objpayload;
          $objname    = "";
          $objpayload = "";
        $progress   = 0;
        }
        else
        {
          if (!$progress) $objname .= $string[$i];
        else
        {
          $progress = 2;
          $objpayload .= $string[$i];
        }
        }
      }
      else
      {
        if ($string[$i] == '\\') $objpayload .= $string[++$i];
        elseif ($string[$i] == '"') $instr = false;
        else $objpayload .= $string[$i];
      }
    } while ($i < strlen($string));
    throw new Exception("Parse error");
  }

  function SimpleDecode($str) { $i = 0; return @_SimpleDecode($str, $i); }

  function FixValues($data)
  {
    $results = array();
    foreach ($data as $k => $v)
    {
      if (is_array($v)) $results[$k] = FixValues($v);
      else if ($k == "date")
      {
        $results[$k] = array(
          "sec"    => (int)substr($v, 0, strlen($v) - 3),
          "milis"  => (int)substr($v, -3),
        );
      }
      else if (($k == "message") || ($k == "name") || ($k == "owner")) $results[$k] = (string)$v;
      else
      {
        // Actuall test?
        if ($k == "custoname") $k = "customname";
        if ($v == "true") $results[$k] = true;
        else if ($v == "false") $results[$k] = false;
        else if (($v == "null") && ($k == "customname")) $results[$k] = null;
        else if (is_numeric($v))
        {
          if (strpos(".", $v) !== false) $results[$k] = (float)$v;
          else $results[$k] = (int)$v;
        }
        else $results[$k] = (string)$v;
      }
    }
    return $results;
  }

  function err($string)
  {
    echo @date(DATE_RFC822).": $string\n";
  }

  function TouchFile($file)
  {
    $dir = explode('/', dirname($file));
    $name = basename($file);
    $ocwd = getcwd();
    if ((!$dir[0]) && (count($dir) > 1)) $dir[0] = "/";
    foreach ($dir as $cwd)
    {
      if ((!is_dir($cwd)) && (!mkdir($cwd))) die("Cant create directory $cwd ($file)! :-(\n");
      chdir($cwd);
    }
    fclose(fopen($name, "a"));
    chdir($ocwd);
  }

  function ReloadProcesses($date)
  {
    global $processes;
    foreach ($processes as $name => $process)
    {
      @fclose($process["pipes"][0]);
      @proc_close($process["pid"]);
      $fname = str_replace("%DATE%", $date, $name);
      err("Opening: $fname");
      TouchFile($fname);
      $descriptorspec = array(
        0 => array("pipe", "r"),
        1 => array("file", "$fname", "a"),
        2 => array("file", "/dev/null", "w")
      );
      $proc_new["pid"] = proc_open("exec ".COMMAND, $descriptorspec, $pipes);
      $proc_new["pipes"] = $pipes;
      $processes[$name] = $proc_new;
    }
  }

  function WriteProcess($name, $data)
  {
    global $processes;
    fwrite($processes[$name]["pipes"][0], json_encode($data)."\n");
  }

  function RouteToDestination($data)
  {
    global $processes, $sort;
    $afname = _DEFAULT;
    // Iterates trough sort untill it finds a suitable bucket
    foreach ($sort as $fname => $sdata)
    {
      try
      {
        if (is_array($sdata))
        {
          foreach ($sdata as $event)
          {
            if ((is_callable($event)) && ($event($data))) { $afname = $fname; break 2; }
            else if ($data["event"] == $event)  { $afname = $fname; break 2; }
          }
        }
        else if ((is_callable($sdata)) && ($sdata($data))) { $afname = $fname; break; }
      }
      catch (Exception $e) { /* OK, OK, i get it. */ }
    }
    WriteProcess($afname, $data);
  }

  $processes = array(_DEFAULT => array("pipes" => array(0)));
  foreach ($sort as $name => $spam)
  {
    $processes[$name] = array("pipes" => array(0));
  }
  $pday = 0;

  $db = new SQLite3(READ_FILE);
  $db->close();
  unset($db);

  while (true)
  {
    $sync_file = fopen(SYNC_FILE, "r");
    flock($sync_file, LOCK_EX);
    flock($sync_file, LOCK_UN);
    fclose($sync_file);
    $_file = fopen(READ_FILE, "a+");
    flock($_file, LOCK_EX);
    $mn = -1;
    $db = new SQLite3(READ_FILE);
    $db->query("PRAGMA synchronous = OFF");
    $db->query("PRAGMA journal_mode = MEMORY");
    $db->query("BEGIN TRANSACTION");
    @$result = $db->query("SELECT * FROM events ORDER BY eventno ASC LIMIT 0,128");
    $data = array();
    if ($result)
    {
      while ($row = $result->fetchArray())
      {
        $data[] = $row;
        $mn = $row["eventno"];
      }
    }
    @$db->query("DELETE FROM events WHERE eventno <= $mn");
    $db->query("END TRANSACTION");
    $db->close();
    unset($db);
    flock($_file, LOCK_UN);
    fclose($_file);
    foreach ($data as $row)
    {
      $line = str_replace("\n", "", $row["eventstr"]);
      try
      {
        if (defined("EXPECT_JSON"))
        {
          $data = json_decode($line, true);
          @$data["date"] = array(
            "sec"    => (int)substr($data["date"], 0, strlen($data["date"]) - 3),
            "milis"  => (int)substr($data["date"], -3),
          );
        }
        else $data = FixValues(SimpleDecode($line));
        if (!isset($data["event"])) throw new Exception("Unknown event :-(");
        $time = $data["date"]["sec"];
        if (!$time) throw new Exception("No timestamp!");
        $day = @date("ymd", $time);
        if ($pday != $day)
        {
          err("Reload log files! ($time - ".@date(DATE_RFC822, $time).")");
          ReloadProcesses($day);
          $pday = $day;
        }
        $data = $filter($data);
        if (!$data) throw new Exception("Filter rejected");
        RouteToDestination($data);
      }
      catch (Exception $e)
      {
        err("Reject [".$e->GetMessage()."]: $line");
      }
    }
    if ($mn == -1) sleep(5);
    else usleep(1);
  }

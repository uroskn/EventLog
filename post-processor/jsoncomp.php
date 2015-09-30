#!/usr/bin/php
<?php

  /**
   *  JCode Bytecode documentation
   *
   *  Opcode:
   *    0x00 : New structrued object, push to stack.
   *    0x01 : <dict#> <entryno> Pop structured object from stack attach to key stored in dict# entryno.
   *    0x02 : Pop from stack, flush to stdout.
   *    0x03 : Assign direct value to current structured object from <dict#> <entryno#>
   *    0x04 : <true/false> Assing boolean true or false to current object.
   *    0x05 : Assign NULL value to current object.
   *
   *    0x10 : Create new dictionary
   *    0x11 : <dict#> Remove dictionary number #.
   *    0x12 : <dict#> Empty dictionary number #.
   *    0x13 : <dict#> <entry#> Remove entry number # from dictionary.
   *
   *    0x20 : <dict#> <word>    Add string to dictionary #.
   *    0x21 : <dict#> <int>     Add 32 bit signed INT to dictionary
   *    0x22 : <dict#> <double>  Add 64 bit double to dictionary
   *
   **/

  class FreeList
  {
    protected $freelist;
    protected $counter;

    function __construct() { $this->counter = 0; $this->freelist = array(); }
    function getNew()
    {
      if (count($this->freelist)) return array_shift($freelist);
      return $this->counter++;
    }

    function outputNumber($num)
    {
      $bytesn = ceil(log($this->counter, 2)/8);
      if (!$bytesn) $bytesn++;
      $result = "";
      for ($i = ($bytesn - 1); $i >= 0; $i--)
      {
        $number = floor($num / pow(256, $i));
        $num = $num - $number*pow(256, $i);
        $result .= chr($number);
      }
      return $result;
    }

    function readNumber($stream)
    {
      $bytesn = ceil(log($this->counter, 2)/8);
      if (!$bytesn) $bytesn++;
      $result = 0;
      for ($i = ($bytesn - 1); $i >= 0; $i--) $result = $result + ord(fread($stream, 1))*pow(256, $i);
      return $result;
    }
  }

  class DictionaryStorage
  {
    static protected $dicts;
    static protected $list;

    static function Init()
    {
      self::$dicts = array();
      self::$list = new FreeList();
    }

    static function createDictionary()
    {
      $id = self::$list->getNew();
      self::$dicts[$id] = new Dictionary();
      return $id;
    }

    static function getDictByID($id)
    {
      return self::$dicts[$id];
    }

    static function printDictID($id)
    {
      echo self::$list->outputNumber($id);
    }
  }

  class ObjStack
  {
    static protected $stack;

    static function Init()
    {
      self::$stack = array();
    }

    static function pushObject($obj = null)
    {
      self::$stack[] = $obj;
    }

    static function popObject()
    {
      return array_pop(self::$stack);
    }

    static function getLast()
    {
      return end(self::$stack);
    }

    static function setLast($obj)
    {
      self::popObject();
      self::pushObject($obj);
    }
  }

  class Dictionary
  {
    protected $dictionary;
    protected $idmap;
    protected $freelist;

    function __construct()
    {
      $this->dictionary = array();
      $this->idmap      = array();
      $this->freelist = new FreeList();
    }

    function objectIndex($object)
    {
      if (empty($this->dictionary[$object])) return -1;
      return $this->dictionary[$object];
    }

    function getByIndex($index)
    {
      if (isset($this->idmap[$index])) return $this->idmap[$index];
      return null;
    }

    function deleteIndex($object)
    {

    }

    function addObject($object)
    {
      $freeid = $this->freelist->getNew();
      $this->dictionary[$object] = $freeid;
      $this->idmap[$freeid]      = $object;
      return $freeid;
    }

    function flush()
    {
    }

    function printSize($int)
    {
      return $this->freelist->outputNumber($int);
    }

    function getSize($stream)
    {
      return $this->freelist->readNumber($stream);
    }

  }

  function RecursiveEncode($obj)
  {
    $keydict = DictionaryStorage::getDictByID(0);
    foreach ($obj as $key => $value)
    {
      echo "\x00";
      if (is_array($value)) RecursiveEncode($value);
      $keyIndex = $keydict->objectIndex($key);
      if ($keyIndex == -1)
      {
        $keyIndex = DictionaryStorage::createDictionary();
        $keydict->addObject($key);
        echo "\x10\x20".$keydict->printSize(0).$key."\x00";
      }
      if (!is_array($value))
      {
        $valuedict = DictionaryStorage::getDictByID($keyIndex);
        if (is_bool($value))
        {
          if ($value) echo "\x04\x01";
          else echo "\x04\x00";
        }
        else if (is_null($value)) echo "\x05";
        else
        {
          $valueid = $valuedict->objectIndex($value);
          if ($valueid == -1)
          {
            if (is_int($value)) echo "\x21".$keydict->printSize($keyIndex).pack("i", $value);
            else if (is_float($value)) echo "\x22".$keydict->printSize($keyIndex).pack("d", $value);
            else
            {
              $value = (string)$value;
              echo "\x20".$keydict->printSize($keyIndex).$value."\x00";
            }
            $valueid = $valuedict->addObject($value);
          }
          echo "\x03".$keydict->printSize($keyIndex).$valuedict->printSize($valueid);
        }
      }
      echo "\x01".$keydict->printSize(0).$keydict->printSize($keyIndex);    }
  }

  function CompressStream($stream)
  {
    echo "\x10\x20\x00";
    ObjStack::Init();
    DictionaryStorage::createDictionary();
    DictionaryStorage::getDictByID(0)->addObject("");
    while (!feof($stream))
    {
      $f = json_decode(fgets($stream), true);
      if (!$f) continue;
      ob_start();
      echo "\x00";
      RecursiveEncode($f);
      echo "\x02";
      ob_end_flush();
    }
  }

  function DecompressStream($stream)
  {
    ObjStack::Init();
    while (!feof($stream))
    {
      $cmd = fread($stream, 1);
      switch ($cmd)
      {
        case "\x00": ObjStack::pushObject(); break;
        case "\x01":
          $keydict    = DictionaryStorage::getDictByID(0);
          $dictionary = DictionaryStorage::getDictByID($keydict->getSize($stream));
          $key        = $dictionary->getByIndex($dictionary->getSize($stream));
          $object     = ObjStack::popObject();
          $lobj       = ObjStack::getLast();
          $lobj[$key] = $object;
          ObjStack::setLast($lobj);
        break;
        case "\x02":
          $object = ObjStack::popObject();
          echo json_encode($object)."\n";
        break;
        case "\x03":
          $keydict    = DictionaryStorage::getDictByID(0);
          $dictionary = DictionaryStorage::getDictByID($keydict->getSize($stream));
          $value      = $dictionary->getByIndex($dictionary->getSize($stream));
          ObjStack::setLast($value);
        break;
        case "\x04":
          if (ord(fread($stream, 1)) > 0) ObjStack::setLast(true);
          else ObjStack::setLast(false);
        break;
        case "\x05":
          ObjStack::setLast(null);
        break;

        case "\x10": DictionaryStorage::createDictionary(); break;
        case "\x11": break;
        case "\x12": break;
        case "\x13": break;

        case "\x20":
          $keydict    = DictionaryStorage::getDictByID(0);
          $dictionary = DictionaryStorage::getDictByID($keydict->getSize($stream));
          $str = "";
          while (true)
          {
            $chr = fread($stream, 1);
            if ($chr == "\x00") break;
            $str .= $chr;
          }
          $dictionary->addObject($str);
        break;
        case "\x21":
          $keydict    = DictionaryStorage::getDictByID(0);
          $dictionary = DictionaryStorage::getDictByID($keydict->getSize($stream));
          $data = unpack("i", fread($stream, 4));
          $dictionary->addObject(array_shift($data));
        break;
        case "\x22":
          $keydict    = DictionaryStorage::getDictByID(0);
          $dictionary = DictionaryStorage::getDictByID($keydict->getSize($stream));
          $data = unpack("d", fread($stream, 4));
          $dictionary->addObject(array_shift($data));
        break;
      }
    }
  }

  if (empty($argv[1])) die("Usage: {$argv[0]} <--compress|--decompress>\nProgram reads at stdin, writes to stdout\n");

  DictionaryStorage::Init();
  if ($argv[1] == "--compress") CompressStream(STDIN);
  else if ($argv[1] == "--decompress") DecompressStream(STDIN);
  else die("Invalid option\n");

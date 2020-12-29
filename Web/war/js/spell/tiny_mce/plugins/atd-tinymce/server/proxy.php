<?php
// LanguageTool Proxy Script
// requires curl for PHP - on Ubuntu, install with "sudo apt-get install php5-curl"
error_reporting(E_ALL);
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $postText = trim(file_get_contents('php://input'));
  $postText = html_entity_decode($postText, ENT_COMPAT, "UTF-8");
  
  $curl = curl_init();
  curl_setopt($curl, CURLOPT_URL, "https://languagetool.org");
  curl_setopt($curl, CURLOPT_PORT, 8081);
  curl_setopt($curl, CURLOPT_SSLVERSION, 3);
  curl_setopt($curl, CURLOPT_POST, true);
  curl_setopt($curl, CURLOPT_POSTFIELDS, $postText);
  curl_setopt($curl, CURLOPT_HEADER, 0);
  curl_setopt($curl, CURLOPT_REFERER, $_SERVER['HTTP_REFERER']);

  header("Content-Type: text/xml; charset=utf-8");
  //for debugging:
  //header("Content-Type: text/plain");
  
  curl_exec($curl);
  curl_close($curl);
} else {
  print "Error: this proxy only supports POST";
}
?>

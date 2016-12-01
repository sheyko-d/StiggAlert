<?php

http_response_code(500);

// Retrieve POST parameters
$android_id = $_POST['android_id'];
$token = $_POST['token'];

if (!isset($android_id) or !isset($token)){
	die("Some fields are empty");
}

// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

$android_id = mysqli_real_escape_string($con, $android_id);
$token = mysqli_real_escape_string($con, $token);

// Update token in database
$db->makeQuery($con, "UPDATE user SET token='$token' WHERE android_id='$android_id'") or die(mysqli_error($con));
http_response_code(200);

?>
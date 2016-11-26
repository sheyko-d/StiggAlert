<?php

http_response_code(500);

// Retrieve POST parameters
if (isset($_POST['user'])){
	$user = json_decode($_POST['user'], true);
	$android_id = $user["android_id"];
	$device_name = $user["device_name"];
	$nickname = $user["nickname"];
}

if (!isset($user)){
	die("Some fields are empty");
}

// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Check if user already exists
$user_query = $db->makeQuery($con, "SELECT android_id, device_name, nickname FROM user WHERE android_id='$android_id'");
$user_result = $user_query->fetch_assoc();
if ($user_result == null){
	$db->makeQuery($con, "INSERT INTO user(android_id, device_name, nickname) VALUES('$android_id', '$device_name', '$nickname')") or die(mysqli_error($con));
	http_response_code(200);
} else {
	http_response_code(200);
}

?>
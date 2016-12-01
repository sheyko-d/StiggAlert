<?php

// Retrieve POST parameters
$android_id = $_POST['android_id'];

if (!isset($android_id)){
	die("Some fields are empty");
}

// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

$android_id = mysqli_real_escape_string($con, $android_id);

// Update token in database
$user_query = $db->makeQuery($con, "SELECT device_name, nickname, sensitivity FROM user WHERE android_id='$android_id'") or die(mysqli_error($con));
$user_result = $user_query->fetch_assoc();
$device_name = $user_result["device_name"];
$nickname = $user_result["nickname"];
$sensitivity = intval($user_result["sensitivity"]);

echo json_encode(array("android_id"=>$android_id, "device_name"=>$device_name, "nickname"=>$nickname, "sensitivity"=>$sensitivity));

http_response_code(200);

?>
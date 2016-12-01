<?php

// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

$android_id = mysqli_real_escape_string($con, $android_id);
$enabled = mysqli_real_escape_string($con, $enabled);

// Get enabled states
$user_query = $db->makeQuery($con, "SELECT android_id, enabled FROM user ORDER BY time ASC");
$users = array();
while ($user_result = $user_query->fetch_assoc()){
	$android_id = $user_result["android_id"];
	$enabled = $user_result["enabled"];
	array_push($users, array("android_id"=>$android_id, "enabled"=>$enabled));
}

echo json_encode($users);

?>
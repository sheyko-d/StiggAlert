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

// Delete user
$db->makeQuery($con, "DELETE FROM user WHERE android_id='$android_id'") or die(mysqli_error($con));

$photo_query = $db->makeQuery($con, "SELECT path, thumbnail_path FROM photo WHERE device_id='$android_id'");
while ($photo_result = $photo_query->fetch_assoc()){
	$path = $photo["path"];
	$thumbnail_path = $photo["thumbnail_path"];
	$path = str_replace("http://stigg.ca/alert/", "../../", $path);
	$thumbnail_path = str_replace("http://stigg.ca/alert/", "../../", $thumbnail_path);
	unlink($path);
	unlink($thumbnail_path);
}

$db->makeQuery($con, "DELETE FROM photo WHERE device_id='$android_id'") or die(mysqli_error($con));

?>
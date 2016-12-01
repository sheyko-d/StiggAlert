<?php

// Retrieve POST parameters
$android_id = $_POST['android_id'];
$enabled = $_POST['enabled'];

if (!isset($android_id) or !isset($enabled)){
	die("Some fields are empty");
}

$enabled = $enabled == "true" ? 1 : 0;

// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

$android_id = mysqli_real_escape_string($con, $android_id);
$enabled = mysqli_real_escape_string($con, $enabled);

// Update nickname in database
$db->makeQuery($con, "UPDATE user SET enabled='$enabled' WHERE android_id='$android_id'") or die(mysqli_error($con));

?>
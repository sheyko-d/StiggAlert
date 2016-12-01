<?php

// Retrieve POST parameters
$android_id = $_POST['android_id'];
$nickname = $_POST['nickname'];

if (!isset($android_id) or !isset($nickname)){
	die("Some fields are empty");
}

// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

$android_id = mysqli_real_escape_string($con, $android_id);
$raw_nickname = $nickname;
$nickname = mysqli_real_escape_string($con, $nickname);

// Update nickname in database
$db->makeQuery($con, "UPDATE user SET nickname='$nickname' WHERE android_id='$android_id'") or die(mysqli_error($con));

$token_query = $db->makeQuery($con, "SELECT token FROM user WHERE android_id='$android_id'") or die(mysqli_error($con));
$token_result = $token_query->fetch_assoc();
$token = $token_result["token"];

sendPushNotification($token, $raw_nickname);

function sendPushNotification($token, $nickname){
	$data = array('type' => 'nickname', 'nickname' => $nickname);
	
	$participant_google_tokens = array();
	array_push($participant_google_tokens, $token);
	
    $apiKey = 'AIzaSyDSEpK81_LHJzk18uirwxLwXOzDkjxBmVQ';

    // Set POST request body
    $post = array(
                    'registration_ids'   => $participant_google_tokens,
                    'data' => $data,
                 );

    // Set CURL request headers 
    $headers = array( 
                        'Authorization: key=' . $apiKey,
                        'Content-Type: application/json'
                    );

    // Initialize curl handle       
    $ch = curl_init();

    // Set URL to GCM push endpoint     
    curl_setopt($ch, CURLOPT_URL, 'https://fcm.googleapis.com/fcm/send');

    // Set request method to POST       
    curl_setopt($ch, CURLOPT_POST, true);

    // Set custom request headers       
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    // Get the response back as string instead of printing it       
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

    // Set JSON post data
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($post));

    // Actually send the request    
    curl_exec($ch);

    // Close curl handle
    curl_close($ch);
}

?>
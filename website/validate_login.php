<?php
session_start();
//$_SESSION['authenticated'] = false;// TODO: REMOVE

$secretpassword = 'admin';
$secretusername = 'admin';

if ($_SESSION['authenticated_alert'] == false) {
	header('Location: login');
	die();
}
?>
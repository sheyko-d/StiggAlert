<?php
session_start();

$secretpassword = 'admin';
$secretusername = 'admin';

if ($_SESSION['authenticated_alert'] == true) {
   header('Location: /alert/');
} else {
   $error = null;
   if (!empty($_POST)) {
       $username = empty($_POST['username']) ? null : $_POST['username'];
       $password = empty($_POST['password']) ? null : $_POST['password'];

       if ($username == $secretusername && $password == $secretpassword) {
           $_SESSION['authenticated_alert'] = true;
           // Redirect to your secure location
           header('Location: /alert/');
           return;
       }
   }
   ?>
<head>
	<title>Login | Stigg Alert Dasboard</title>
	<link rel="stylesheet" type="text/css" href="css/style.css"/>
	<link href='http://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>
	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon">
	<link rel="icon" href="images/favicon.ico" type="image/x-icon">
	<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
</head>
<body>	
	<div class="container">
  <form action="login.php" method="post" id="login_form">
    <h1 style="color: #333">
		<i class="material-icons" style="font-size:35px; vertical-align: middle; padding-bottom:10px; padding-right:2px">camera</i>
		Stigg Alert
	</h1>
    <div class="form-group">
      <input type="text" name="username" id="username" required/>
      <label for="input" class="control-label">Username</label><i class="bar"></i>
    </div>    
    <div class="form-group">
      <input type="password" name="password" required/>
      <label for="input" class="control-label">Password</label><i class="bar"></i>
    </div>
	<? if (isset($username) and isset($password) and !($username == $secretusername && $password == $secretpassword)) {?>
	<div style="color:#F44336">
		Incorrect username or password.
	</div>
	<? } ?>
  </form>
  <div class="button-container">
    <button type="button" class="button" id="login_button"><span>Submit</span></button>
  </div>
</div>
</body>

<script>
	$("#login_button").click(function() {
		$("#login_form").submit();
	});
	
	$(document).keypress(function(e) {
		if(e.which == 13) {
			$("#login_form").submit();
		}
	});
	
	$( document ).ready(function() {
		$("#username").focus();
	});
</script>
<?php
}
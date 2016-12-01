<?
require "validate_login.php";

if (isset($_GET["time"])){
	$group_time = $_GET["time"];
}
date_default_timezone_set("MST");
$today_start_ts = strtotime(date('Y-m-d', time()). '00:00:00');
$yesterday_start_ts = $today_start_ts-24*60*60;
$day_1_ago_start_ts = $yesterday_start_ts-24*60*60;
$day_2_ago_start_ts = $day_1_ago_start_ts-24*60*60;
$day_3_ago_start_ts = $day_2_ago_start_ts-24*60*60;
$day_4_ago_start_ts = $day_3_ago_start_ts-24*60*60;
$day_5_ago_start_ts = $day_4_ago_start_ts-24*60*60;
$day_6_ago_start_ts = $day_5_ago_start_ts-24*60*60;

if (isset($_POST["start_date"])){
	$start_date = strtotime($_POST["start_date"]);
}
if (isset($_POST["end_date"])){
	$end_date = strtotime($_POST["end_date"]);
}

if ($start_date == null){
	$start_date = $today_start_ts-6*24*60*60;
}
if ($end_date == null){
	$end_date = $today_start_ts+24*60*60;
}
			
// Connect to the database
require_once("util/database.php");
$db = new DBConnect();
$con = $db->openConnection();
?>

<head>
	<title>Stigg Alert Dashboard</title>
	<link rel="stylesheet" type="text/css" href="css/style.css"/>
	<link href='http://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>
	<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
	<link rel="shortcut icon" href="images/favicon.png" type="image/x-icon">
	<link rel="icon" href="images/favicon.png" type="image/x-icon">
	<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
	<link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
	<link rel="stylesheet" href="css/material.css">
	<script defer src="https://code.getmdl.io/1.2.1/material.min.js"></script>
</head>
<body>
	<div id="navigation_drawer">
		<div id="drawer_header">
			<a href="/alert/" class="logo">
				<i class="material-icons" style="font-size:30px; vertical-align: middle; padding-bottom:8px; padding-right:6px">camera</i>
				Stigg Alert
			</a>
		</div>
		<div style="padding: 10px 0px 10px 0px; overflow-y: scroll; height:calc(100% - 220px)">
		<?
			$user_query = $db->makeQuery($con, "SELECT android_id, device_name, nickname FROM user ORDER BY time ASC");

			$first_android_id = null;
			$position = 1;
			while ($user_result = $user_query->fetch_assoc()){
				if ($first_android_id == null){
					$first_android_id = $user_result["android_id"];
				}
				$device_name = $user_result["device_name"];
				?>

				<?				
					echo "<a href='/alert/?user=".$user_result['android_id']."' class='user'>";?>
					<table style="width: 100%" cellspacing="0" cellpadding="0" class="user_wrapper">
						<div>
							<td style="padding: 15px 10px 15px 20px">
								<div class="user_photo" style="background:#c52127;border-radius:100px; color:#ffffff; font-size:30px; text-align:center; padding-top:6px"><?echo $position?></div>
							</td>
							<td class='user_info' style="padding: 10px 20px 10px 10px">
								<div><? echo $user_result["device_name"]?></div>
								<div class='user_assigned_object'><? echo !empty($user_result["nickname"])?$user_result["nickname"]:"No nickname"?></div>
							<td>
						</div>
					</table>
				<?
				$position++;
			}
		?>
		</div>
		<div style="padding: 10px 0px; position: absolute; bottom: 0; left: 0; width: 100%">
			<a href="devices" style="text-decoration: none">
				<div class="menu_item_selected">Manage Devices</div>
			</a>
		</div>
	</div>
	<div id="content">
		<div id="content_header">Manage Devices</div>
		<table id="content_table">
			<tr>
				<td class="block_inactive" style="padding:10px">
					<table style="width:100%" cellspacing="0" cellpadding="0" id="manage_table">
						<tr>
							<th>Model</th>
							<th>Nickname</th>
							<th>Last photo</th>
							<th>Camera Sensitivity</th>
							<th style="text-align:center">Surveliance</th>
							<th style="text-align:center">Delete</th>
						</tr>
							<?
							$user_query = $db->makeQuery($con, "SELECT android_id, sensitivity, device_name, nickname, enabled FROM user ORDER BY time ASC");

							while ($user_result = $user_query->fetch_assoc()){
								$device_name = $user_result["device_name"];
								$sensitivity = $user_result["sensitivity"];								
								$android_id = $user_result["android_id"];
								$enabled = $user_result["enabled"];
								
								$photo_query = $db->makeQuery($con, "SELECT path, thumbnail_path, UNIX_TIMESTAMP(time) AS timestamp FROM photo WHERE device_id='$android_id' ORDER BY time DESC");
								if ($photo_result = $photo_query->fetch_assoc()){
									$time = $photo_result["timestamp"];
								} else {
									$time = null;
								}
								?>
								<tr>
									<td style="padding:10px">
										<? echo $user_result["device_name"]?>
									</td>
									<td style="padding:10px; width: 22%">
										<input class="mdl-textfield__input" value="<? echo $user_result["nickname"]?>" type="text" placeholder="Enter nickname..." android_id="<?echo $android_id?>">
									</td>
									<td style="padding:10px">
										<? echo $time!=null ? date("M j, h:i A", $time) : "—"?>
									</td>
									<td style="padding:10px; width:300px">
										<div style="margin-left:-25px; margin-right: 40px">
											<input class="mdl-slider mdl-js-slider" type="range" min="0" max="100" value="<?echo $sensitivity?>" step="1" android_id="<?echo $android_id?>" title="Change camera sensitivity">
										</div>
									</td>
									<td style="padding:10px; text-align:center">
										<i class="toggle_surveilance material-icons" style="font-size:25px; vertical-align: middle; padding:0 10px; color:#c52127; cursor:pointer" id="<?echo $android_id?>" android_id="<?echo $android_id?>" device_enabled="<?echo $enabled?>" title="<?echo $enabled=="0"?"Start surveillance":"Stop surveillance"?>"><?echo $enabled=="0"?"play_arrow":"pause"?></i>
									</td>
									<td style="padding:10px; text-align:center">
										<i class="delete_user material-icons" style="font-size:23px; vertical-align: middle; padding:0 10px; color:#c52127; cursor:pointer" android_id="<?echo $android_id?>" title="Delete this device">close</i>
									</td>
								</tr>
								<?
							}
							?>
					</table>
				</td>
			</tr>
		</table>
	</div>
</body>
<footer>
	<script>
		function myFunction(){
			document.getElementById("data_form").submit();
		}
		
		function deleteCollection(time, user_id){
			$.ajax({
				url: 'api/photo/delete_photos.php',
				type: 'POST',
				data: {
					time: time,
					user_id: user_id
				},
				success: function (data) {
					location.reload();
				}
			});
		}
		
		$(".mdl-textfield__input").on('keyup change', function() {			
			var android_id = $(this).attr('android_id');
			var nickname = $(this).val();
			
			$.ajax({
				url: 'api/user/update_nickname.php',
				type: 'POST',
				data: {
					android_id: android_id,
					nickname: nickname
				}
			});
		});
		
		$(".mdl-slider").change(function() {
			var android_id = $(this).attr('android_id');
			var sensitivity = $(this).val();
			
			$.ajax({
				url: 'api/user/update_sensitivity.php',
				type: 'POST',
				data: {
					android_id: android_id,
					sensitivity: sensitivity
				}
			});
		});
		
		$(".toggle_surveilance").click(function() {							
			var android_id = $(this).attr('android_id');
			var enabled = $(this).attr('device_enabled');
			
			$("#"+android_id).html("cached");
			
			$.ajax({
				url: 'api/user/toggle_surveillance.php',
				type: 'POST',
				data: {
					android_id: android_id,
					enabled: enabled
				}
			});
		});
		
		$(".delete_user").click(function() {
			if (!confirm("Delete this device and all photos?")) return;
			
			var android_id = $(this).attr('android_id');
			
			$.ajax({
				url: 'api/user/delete_user.php',
				type: 'POST',
				data: {
					android_id: android_id
				},
				success: function (data) {
					location.reload();
				}
			});
		});
		
		$(document).ready(function() {
			setInterval(function() {
				$.ajax({					
					dataType: "json",
					url: 'api/user/get_enabled_states.php',
					type: 'GET',
					success: function (data) {
						$.each(data, function(i, user) {
							$("#"+user.android_id).html(user.enabled=="1"?"pause":"play_arrow");
							$("#"+user.android_id).attr("device_enabled", user.enabled);
						});
					}
				});
			}, 2000);
		});
	</script>
</footer>
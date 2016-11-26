<?
require "validate_login.php";

if (isset($_GET["time"])){
	$group_time = $_GET["time"];
}

date_default_timezone_set('GMT-6');
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
	$start_date = $today_start_ts;
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
	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon">
	<link rel="icon" href="images/favicon.ico" type="image/x-icon">
	<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
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
			$user_query = $db->makeQuery($con, "SELECT android_id, device_name, nickname FROM user");

			$first_android_id = null;
			while ($user_result = $user_query->fetch_assoc()){
				if ($first_android_id == null){
					$first_android_id = $user_result["android_id"];
				}
				$device_name = $user_result["device_name"];
				?>

				<?
				$id = $_GET["user"];
				if ($id == null){
					$id = $first_android_id;
				}
				if ($user_result["android_id"] != $id) {
					echo "<a href='/alert/?user=".$user_result['android_id']."' class='user'>";
				}?>
					<table style="width: 100%" cellspacing="0" cellpadding="0" class="<?
						
						echo $user_result["android_id"] == $id ? "user_wrapper_selected" : "user_wrapper"?>
						">
						<div>
							<td style="padding: 15px 10px 15px 20px">
								<div class="user_photo" style="background:#c52127;border-radius:100px; color:#ffffff; font-size:30px; text-align:center; padding-top:6px">1</div>
							</td>
							<td class='user_info' style="padding: 10px 20px 10px 10px">
								<div><? echo $user_result["device_name"]?></div>
								<div class='user_assigned_object'><? echo !empty($user_result["nickname"])?$user_result["nickname"]:"Click to add nickname"?></div>
							<td>
						</div>
					</table>
				<?if ($user_result["android_id"] != $id) {
					echo "</a>";
				}?>
				<?
			}
		?>
		</div>
	</div>
	<div id="content">
		<div id="content_header">Dashboard
			<div style="float: right">
				<form class="content_date" method="post" id="data_form">
					<font style="margin: 0px 10px">View data from</font>
					<input type="date" name="start_date" id="start_date" value="<?echo date('Y-m-d', $start_date)?>" max="<?echo date('Y-m-d', $today_start_ts-60*60*24)?>" oninput="myFunction()"/>
					<font style="margin: 0px 10px">to</font>
					<input type="date" name="end_date" value="<?echo date('Y-m-d', $end_date)?>" max="<?echo date('Y-m-d', $today_start_ts)?>"/>
				</form>
			</div>
		</div>
		<table id="content_table">
			<?
			$COLLECTIONS_GROUP_MIN = 60;
			
			$photo_query = $db->makeQuery($con, "SELECT path, time FROM photo ORDER BY time DESC");
			
			$photos = array();
			while ($photo_result = $photo_query->fetch_assoc()){
				$path = $photo_result["path"];
				$time = strtotime($photo_result["time"]);
				
				array_push($photos, array("path"=>$path, "time"=>$time));
			}
			
			$collections = array();
			$collection = array();
			for ($i=0; $i<count($photos); $i++){
				if ((!isset($_GET["time"]) and $photos[$i]["time"] - $photos[$i+1]["time"] > $COLLECTIONS_GROUP_MIN*60) or (isset($_GET["time"]) and abs($group_time - $photos[$i]["time"]) > $COLLECTIONS_GROUP_MIN*60)){
					// Photo after interval, create new collection
					array_push($collection, $photos[$i]);
					array_push($collections, $collection); 
					$collection = array();
				} else {
					if (isset($_GET["time"])){
						$group_time = $photos[$i]["time"];
					}
					
					// Photo within interval
					$photo = $photos[$i];
					array_push($collection, $photo);
				}
			}
			if (!empty($collection)){
				array_push($collections, $collection); 
			}
			
			$position = 0;
			foreach ($collections as $collection){
				$path = $collection[0]["path"];
				$time = $collection[0]["time"];
				if ($position==0) {echo "<tr>";}
				?>
					<td>
						<a href="view?time=<?echo $time?>">
							<div class="block" style="padding-bottom:11px">
								<table style="width: 100%">
										<tr>
											<td>
												<div id="steps_chart" class="block_body">											
													<a href="view?time=<?echo $time?>" class="collection_title">
														<div style="background-image: url('<?echo $path?>'); height:280px; width:100%; background-position: center; background-size: cover; border-radius:2px"></div>
													</a>
												</div>
											</td>
										</tr>
										<tr>
											<td>
												<h4 style="padding:13px 0 0 0; margin:0; float:left">
													<i class="material-icons" style="font-size:19px; vertical-align: middle; padding-bottom:5px; margin-left:-3px; padding-right:2px">alarm</i>
													<?
													$start_time = gmdate("h:i A", $collection[count($collection)-1]["time"]);
													$end_time = gmdate("h:i A", $time);
													if ($start_time!=$end_time){
														echo $start_time." - ".$end_time;
													} else {
														echo $start_time;
													}
													?>
												</h4>
												<?  if (count($collection)>1){?>
													<h4 style="padding:13px 0 0 0; margin:0; float:right">
														<i class="material-icons" style="font-size:16px; vertical-align: middle; padding-bottom:3.5px; margin-left:-3px; padding-right:2px">collections</i>
														<? echo (count($collection))." photos" ?>		
													</h4>
												<?} else {?>	
													<h4 style="padding:13px 0 0 0; margin:0; float:right">
														<i class="material-icons" style="font-size:17px; vertical-align: middle; padding-bottom:3.5px; margin-left:-3px; padding-right:2px">photo</i>
														<? echo "1 photo" ?>		
													</h4>
												<?}?>
											</td>
										</tr>
								</table>
							</div>
						</a>
					</td>
				<?
				if ($position==2){echo "</tr>";}
				$position++;
				if ($position > 2){
					$position = 0;
				}
			}
			while ($position % 3 != 0){
				?>				
				<td style="padding: 20px;">
				</td>
				<?
				$position++;
			}
			?>
			
			

		</table>
	</div>
</body>
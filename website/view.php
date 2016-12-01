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
	<link rel="shortcut icon" href="images/favicon.png" type="image/x-icon">
	<link rel="icon" href="images/favicon.png" type="image/x-icon">
	<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script>
	<link href="lightgallery/dist/css/lightgallery.css" rel="stylesheet">
</head>
<body>
	<div id="navigation_drawer">
		<div id="drawer_header">
			<a href="/alert/" class="logo">
				<i class="material-icons" style="font-size:32px; vertical-align: middle; padding-bottom:8px; padding-right:4px">arrow_back</i>
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
								<div class="user_photo" style="background:#c52127;border-radius:100px; color:#ffffff; font-size:30px; text-align:center; padding-top:6px"><?echo $position?></div>
							</td>
							<td class='user_info' style="padding: 10px 20px 10px 10px">
								<div><? echo $user_result["device_name"]?></div>
								<div class='user_assigned_object'><? echo !empty($user_result["nickname"])?$user_result["nickname"]:"No nickname"?></div>
							<td>
						</div>
					</table>
				<?if ($user_result["android_id"] != $id) {
					echo "</a>";
				}
				$position++;
			}
		?>
		</div>
	</div>
	<div id="content">
	<?
			$COLLECTIONS_GROUP_MIN = 60;
			
			$id = $_GET["user"];
			if ($id == null){
				$id = $first_android_id;
			}
			$photo_query = $db->makeQuery($con, "SELECT path, thumbnail_path, UNIX_TIMESTAMP(time) AS timestamp FROM photo WHERE device_id='$id' ORDER BY time DESC");
			
			$photos = array();
			while ($photo_result = $photo_query->fetch_assoc()){
				$path = $photo_result["path"];
				$thumbnail_path = $photo_result["thumbnail_path"];
				$time = $photo_result["timestamp"];
				
				array_push($photos, array("path"=>$path, "thumbnail_path"=>$thumbnail_path, "time"=>$time));
			}
			
			$collections = array();
			$collection = array();
			for ($i=0; $i<count($photos); $i++){
				if (abs($group_time - $photos[$i]["time"]) > $COLLECTIONS_GROUP_MIN*60){
					// Photo after interval, create new collection
					if (!empty($collection)){
						array_push($collections, $collection); 
					}
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
			
		?>
		<div id="content_header">
			<?
			$collection = $collections[0];
			$start_time = date("g A", $collection[count($collection)-1]["time"]);
			$end_time = date("g A", $collection[0]["time"]);
			if ($start_time!=$end_time){
				echo date("M j, ", $collection[count($collection)-1]["time"]).$start_time." - ".$end_time;
			} else {
				echo date("M j, ", $collection[count($collection)-1]["time"]).$start_time;
			}
			?>
		</div>
		
		<div id="grid">
			<?
			foreach ($collections as $collection){
				foreach ($collection as $photo){
					$path = $photo["path"];
					$thumbnail_path = $photo["thumbnail_path"];
					$time = $photo["time"];
					?>
					<div class="grid_cell" data-src="<?echo $path?>" data-sub-html="<h4><? echo date("h:i A", $time) ?></h4>">
						<div style="background-image: url('<?echo $thumbnail_path?>'); background-position: center; background-size: cover"></div>
						<h4 style="padding:13px 0 0 0; margin:0; float:left; font-size:14px">
							<i class="material-icons" style="font-size:15px; vertical-align: middle; padding-bottom:4.5px; margin-left:-3px; padding-right:1px">alarm</i>
							<? echo date("h:i A", $time) ?>
						</h4>
					</div>
					<?
				}
			}
			
			?>
		</div>
	</div>
    <script type="text/javascript">
    $(document).ready(function(){
        $('#grid').lightGallery();
    });
    </script>
    <script src="https://cdn.jsdelivr.net/picturefill/2.3.1/picturefill.min.js"></script>
    <script src="lightgallery/src/js/lightgallery.js"></script>
	<script src="lightgallery/src/js/lg-fullscreen.js"></script>
	<script src="lightgallery/src/js/lg-thumbnail.js"></script>
	<script src="lightgallery/src/js/lg-video.js"></script>
	<script src="lightgallery/src/js/lg-autoplay.js"></script>
	<script src="lightgallery/src/js/lg-zoom.js"></script>
	<script src="lightgallery/src/js/lg-hash.js"></script>
	<script src="lightgallery/src/js/lg-pager.js"></script>
	<script src="lightgallery/lib/jquery.mousewheel.min.js"></script>
</body>
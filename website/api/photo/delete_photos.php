<?

$group_time = $_POST["time"];
$user_id = $_POST["user_id"];

date_default_timezone_set("MST");

if (!isset($group_time) or !isset($user_id)){
	die("Some fields are empty");
}
			
// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

$photo_query = $db->makeQuery($con, "SELECT photo_id, path, thumbnail_path, UNIX_TIMESTAMP(time) AS timestamp FROM photo WHERE device_id='$user_id' ORDER BY time DESC");
			
$photos = array();
while ($photo_result = $photo_query->fetch_assoc()){
	$photo_id = $photo_result["photo_id"];
	$path = $photo_result["path"];
	$thumbnail_path = $photo_result["thumbnail_path"];
	$time = $photo_result["timestamp"];
	
	array_push($photos, array("photo_id"=>$photo_id, "path"=>$path, "thumbnail_path"=>$thumbnail_path, "time"=>$time));
}

$COLLECTIONS_GROUP_MIN = 60;

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
		$group_time = $photos[$i]["time"];
		
		// Photo within interval
		$photo = $photos[$i];
		array_push($collection, $photo);
	}
}
	
if (!empty($collection)){
	array_push($collections, $collection);
}
	
foreach ($collections as $collection){
	foreach ($collection as $photo){
		$photo_id = $photo["photo_id"];
		$path = $photo["path"];
		$thumbnail_path = $photo["thumbnail_path"];
		$path = str_replace("http://stigg.ca/alert/", "../../", $path);
		$thumbnail_path = str_replace("http://stigg.ca/alert/", "../../", $thumbnail_path);
		unlink($path);
		unlink($thumbnail_path);
		
		$db->makeQuery($con, "DELETE FROM photo WHERE photo_id='$photo_id'");
	}
}

?>
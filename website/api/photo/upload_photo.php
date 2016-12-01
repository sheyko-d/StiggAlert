<?php

// Retrieve POST parameters
if (isset($_POST['android_id'])){
	$android_id = json_decode($_POST['android_id']);
}
	
$photos = array();


// Connect to the database
require_once("../../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();
	
$name = generateFileName().".".pathinfo($_FILES['photo']['name'], PATHINFO_EXTENSION);
	
$file_path = "../../uploads/".$name;
	
if(move_uploaded_file($_FILES['photo']['tmp_name'], $file_path)) {
	createThumbnail($name);
	
	$result = array("result" => "success", "value" => $var);
	$photo = "http://".$_SERVER["HTTP_HOST"]."/alert/uploads/".$name;
	$thumbnail = "http://".$_SERVER["HTTP_HOST"]."/alert/uploads/thumbs/".$name;
	
	$db->makeQuery($con, "INSERT INTO photo(path, thumbnail_path, device_id) VALUES('$photo', '$thumbnail', '$android_id')") or die(mysqli_error($con));
} else{
	die ("Can't move photo");
}

function generateFileName() {
	$length = 20;
    $characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $charactersLength = strlen($characters);
    $randomString = '';
    for ($i = 0; $i < $length; $i++) {
        $randomString .= $characters[rand(0, $charactersLength - 1)];
    }
    return $randomString;
}

function createThumbnail($filename) {
    $final_width_of_image = 300;
	$path_to_image_directory = '../../uploads/';
	$path_to_thumbs_directory = '../../uploads/thumbs/';
     
    if(preg_match('/[.](jpg)$/', $filename)) {
        $im = imagecreatefromjpeg($path_to_image_directory . $filename);
    } else if (preg_match('/[.](gif)$/', $filename)) {
        $im = imagecreatefromgif($path_to_image_directory . $filename);
    } else if (preg_match('/[.](png)$/', $filename)) {
        $im = imagecreatefrompng($path_to_image_directory . $filename);
    }
     
    $ox = imagesx($im);
    $oy = imagesy($im);
     
    $nx = $final_width_of_image;
    $ny = floor($oy * ($final_width_of_image / $ox));
     
    $nm = imagecreatetruecolor($nx, $ny);
     
    imagecopyresized($nm, $im, 0,0,0,0,$nx,$ny,$ox,$oy);
     
    if (!file_exists($path_to_thumbs_directory)) {
		if (!mkdir($path_to_thumbs_directory)) {
			die("There was a problem. Please try again!");
		} 
    }
 
    imagejpeg($nm, $path_to_thumbs_directory . $filename);
}

?>
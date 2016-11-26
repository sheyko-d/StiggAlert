<?php

class DBConnect {
	
	function DBConnect() {
		$this->host = "localhost";
		$this->username = "stiggca_android";
		$this->password = "Stiggrules511!";
		$this->database = "stiggca_alert";
	}
	
	function openConnection(){
		$con = mysqli_connect($this->host,$this->username,$this->password,$this->database);
		if (!$con){
			die("Could not connect: ".mysqli_error($con));
		}
		return $con;
	}
	
	function makeQuery($con, $query) {
		$query = mysqli_query($con, $query) or die('Error in query execution: '.mysqli_error($con));
		return $query;
	}
	
	function countResults($query) {
		return mysqli_num_rows($query);
	}
	
	function closeConnection($con)	{
		mysqli_close($con);
	}
	
}

?>
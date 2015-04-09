<?php
	require_once 'countries.php';

	$dsn = 'mysql:host=nearme-user.co7uoobllvol.us-east-1.rds.amazonaws.com;port=3306;dbname=nearme_user';
	$username = 'android_nearme';
	$password = 'CuT3d3ViL';

	try {
		$dbh = new PDO($dsn, $username, $password);
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}
	
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	$_POST = json_decode(file_get_contents("php://input"));
	$sourceId = $_POST->sourceId;
	$targetIds = $_POST->targetIds;
	$targetIdsArray =  explode(",", $targetIds);

	$lats = array();
	$lons = array();
	$rTargetIds = array();

	foreach ($targetIdsArray as $id) {
		try {
			$sth = $dbh->prepare('SELECT * FROM user_info WHERE ID = '.$id);
			$sth->execute();
		}
		catch(Exception $e)
		{
			echo $e->getMessage();
		}

		$rows = $sth->fetchAll();

		if(sizeof($rows) && $rows[0]['lat'] != null && $rows[0]['lon'] != null)
		{
			array_push($lats, $rows[0]['lat']);
			array_push($lons, $rows[0]['lon']);
			array_push($rTargetIds, $rows[0]['ID']);
		}
	}

	echo '{ "status" : 1 ,"sourceId" : '.$sourceId.' , "lons" : "'.implode(",", $lons).'", "lats" : "'.implode(",", $lats).'", "targetIds" : "'.implode(",", $rTargetIds).'"}';

	?>
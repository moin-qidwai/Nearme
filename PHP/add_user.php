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
	$username = $_POST->targetUsername;

	try {
		$sth = $dbh->prepare('SELECT * FROM user_info WHERE name = "'.$username.'"');
		$sth->execute();
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}

	$rows = $sth->fetchAll();

	if(sizeof($rows))
	{
		try {
			$ssth = $dbh->prepare('SELECT * FROM user_relations WHERE source_id = '.$sourceId.' and target_id = '.$rows[0]['ID']);
			$ssth->execute();
		}
		catch(Exception $e)
		{
			echo $e->getMessage();
		}

		$secrows = $ssth->fetchAll();
		if(!sizeof($secrows))
		{
			$sql = "INSERT INTO user_relations (source_id,target_id) VALUES (:source_id,:target_id)";

			$q = $dbh->prepare($sql);
			$q->execute(array(':source_id'=>$sourceId,
			                  ':target_id'=>$rows[0]['ID']));
			echo '{ status : 1, targetId : '.$rows[0]['ID'].', username : "'.$rows[0]['name'].'", displayName : "'.$rows[0]['display_name'].'"}';
		}
		else
		{
			echo '{ status : 0 , message : "The contact has already been added" }';
		}
	}
	else
	{
		echo '{ status : 0 , message : "The contact does not exist. Invite them to install nearme" }';
	}

	

?>
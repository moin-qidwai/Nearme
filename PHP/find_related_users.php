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
	$phoneNumbers = $_POST->numbers;
	$sourceId = $_POST->sourceId;
	$countryCode = $_POST->countryCode;

	$ids = array();
	$dpnames = array();
	$usernames = array();

	$sourceOnlyQuery = $dbh->prepare('SELECT * FROM user_relations WHERE source_id = '.$sourceId);
	$sourceOnlyQuery->execute();

	$sourceOnlyRows = $sourceOnlyQuery->fetchAll();

	if(sizeof($sourceOnlyRows))
	{
		foreach ($sourceOnlyRows as $row) {
			try {
				$sth = $dbh->prepare('SELECT * FROM user_info WHERE ID = '.$row['target_id']);
				$sth->execute();
			}
			catch(Exception $e)
			{
				echo $e->getMessage();
			}

			$innerRows = $sth->fetchAll();
			if (sizeof($innerRows)) {
				array_push($ids, $innerRows[0]['ID']);
				array_push($dpnames, $innerRows[0]['display_name']);
				array_push($usernames, $innerRows[0]['name']);
			}
		}
	}
	else
	{
		$numberString =  substr(json_encode($phoneNumbers), 1, sizeof($phoneNumbers)-2);
		$numberArray = explode(",", $numberString);

		foreach ($numberArray as $number) {
			//Do preprocessing of number here to make sure it is in correct state

			$number = str_replace("-", "", $number);
			$number = str_replace(" ", "", $number);
			$number = str_replace("[", "", $number);
			$number = str_replace("]", "", $number);

			if(strpos($number, "+") !== false)
			{
				//do nothing to international number
			}
			else
			{
				$number = "+".$extensions[$countryCode].$number;
			}

			try {
				$sth = $dbh->prepare('SELECT * FROM user_info WHERE number = "'.$number.'" AND ID != '.$sourceId);
				$sth->execute();
			}
			catch(Exception $e)
			{
				echo $e->getMessage();
			}

			$rows = $sth->fetchAll();

			if(sizeof($rows))
			{
				$targetId = $rows[0]['ID'];
				$ssth = $dbh->prepare('SELECT * FROM user_relations WHERE source_id = '.$sourceId.' AND target_id = '.$targetId);
				$ssth->execute();

				$rowss = $ssth->fetchAll();

				if (sizeof($rowss)) {
					array_push($ids, $rows[0]['ID']);
					array_push($dpnames, $rows[0]['display_name']);
					array_push($usernames, $rows[0]['name']);
				}
				else
				{
					$sql = "INSERT INTO user_relations (source_id,target_id) VALUES (:source_id,:target_id)";

					$q = $dbh->prepare($sql);
					$q->execute(array(':source_id'=>$sourceId,
					                  ':target_id'=>$targetId));

					array_push($ids, $rows[0]['ID']);
					array_push($dpnames, $rows[0]['display_name']);
					array_push($usernames, $rows[0]['name']);
				}
			}	
		}
	}

	$ids = implode("," , $ids);
	$dpnames = implode(",", $dpnames);
	$usernames = implode(",", $usernames);

	// debug this shit to see how to return the final numbers array
	echo '{ "status" : '.true.' , "ids" : "'.$ids.'", "names" : "'.$dpnames.'", "usernames" : "'.$usernames.'" }';

	?>
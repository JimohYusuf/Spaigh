# [Spaigh]

`version 1.0.0`

---
## Setting up LAMP server
---
Follow the instructions in the link below to setup a **LAMP** server:

`https://www.digitalocean.com/community/tutorials/how-to-install-linux-apache-mysql-php-lamp-stack-on-ubuntu-20-04`

---
## Setting up database
---
After setting up the **LAMP** server, create a database and in the database, create a table

Name the database `spaigh_database` and the table `spaigh_table`

Create **three** columns in your **table**

Name the columns `time_stamp`, `device_state`, `call_state`. Make sure to set `time_stamp` as the Primary key.

And that's it for the database!

---
## Server side scripting using PHP
---
Create a PHP script in your `www/var/project_name` folder. Note that `project_name` is only necessary if you created a
separate folder different from the default while setting up your server.

Name the PHP script `spaigh_data.php`

Now open `spaigh_data.php` with your preferred code editor. Note that you need **sudo** permission to create
and edit this file.

Copy the below PHP code into the file and save: 

```
<?php
$user_name = "your_database_username";
$password = "your_database_password";
$server = "localhost";
$db_name = "spaigh_database";
$table_name = "spaigh_table";

$con = mysqli_connect($server, $user_name, $password, $db_name);

if($con){
        $time_stamp = $_POST['time_stamp'];
        $device_state = $_POST['device_state'];
        $call_state = $_POST['call_state'];

        $query = "INSERT INTO spaigh_table VALUES ('".$time_stamp."', '".$device_state."', '".$call_state."');";
        $result = mysqli_query($con,$query);
        if($result){
                $status = 'OK';
        } else{
                $status = 'FAILED';
        }
        }

else{$status = 'FAILED';}

echo json_encode(array("response"=>$status));
        mysqli_close($con);
?>

```

---
## Setting up the application
---
* Install and run the application

* Make sure that your **phone** and **server** are connected to the **same** LAN

* Go to **settings** and grant the application `Phone` permission

* Get the server's IP address by running `ifconfig` command on a terminal in the server

* Now, after opening the app, in the text box hinting **server address**, type in `http://your_server_ip_address/spaigh_data.php`

* Note that you might have to add some additional endpoints to the URL above depending on where your PHP script is located
relative to your localhost director

* Now, click the `start service` button to start the app service.

* You can close the app and the service will run in the foreground with a notification at the top of your screen

* In case you want to stop the service, open the app again and click the `stop service` button. 

* And that's it, check your database to see the collected data. 

---
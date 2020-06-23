# [Spaigh]

`version 1.0.0`

---
## Setting up LAMP server
---
Follow the instructions in the URL below to setup a **LAMP** server:

`https://www.digitalocean.com/community/tutorials/how-to-install-linux-apache-mysql-php-lamp-stack-on-ubuntu-20-04`

Or run the following commands on a terminal: note that you need sudo permission
``` 
    //Installing apache server
    sudo apt update
    sudo apt install apache2
    sudo ufw allow in "Apache"
    
    //Installing mysql and PHP
    sudo apt install mysql-server
    sudo mysql_secure_installation // answer yes in all the [y/n] questions EXCEPT the first question (VALIDATE PASSWORD PLUGIN)
                                   // just press enter to continue
                                   // you'll be prompted to create a password, make sure to 
                                   // remember you password as you'll need that to log into your
                                   // database later 
    sudo apt install php libapache2-mod-php php-mysql
    
    //Creating a virtual host
    sudo mkdir /var/www/spaigh
    sudo chown -R $USER:$USER /var/www/spaigh
    sudo nano /etc/apache2/sites-available/spaigh.conf

```
Paste the following code in the file `spaigh.conf` that opens up (use ctrl+shift+v to paste,
                                                                  press ctrl+x to save
                                                                  enter Y
                                                                  then press enter)   

```
<VirtualHost *:80>
    ServerName spaigh
    ServerAlias www.spaigh
    ServerAdmin webmaster@localhost
    DocumentRoot /var/www/spaigh 
    ErrorLog ${APACHE_LOG_DIR}/error.log
    CustomLog ${APACHE_LOG_DIR}/access.log combined
</VirtualHost>

```

Then run in your terminal: 

```
   sudo a2ensite spaigh  //ignore any warnings 
   sudo a2dissite 000-default
   sudo systemctl reload apache2 
```
---
## Setting up database
---
After setting up the **LAMP** server, create a database and in the database, create a table

Name the database `spaigh_database` and the table `spaigh_table`

Create **three** columns in your **table**

Name the columns `time_stamp`, `device_state`, `call_state`. Make sure to set `time_stamp` as the Primary key.

And that's it for the database!

Follow the steps below to execute the instructions above: 

First open a terminal (key combination: ctrl + alt + del) 

Then follow the below steps line by line: 

```
sudo mysql //then enter your root password (your computer's password, NOT the password you set earlier) if requested

//if you encounter an error, use `sudo mysql -p` instead and enter the root password that you set 

//First create the database 
CREATE DATABASE spaigh_database;

//See your databases (you should see the database name `spaigh_database` in the list). If not, you might have to recreate the database
//restarting from the //First create a database step.
show databases;

//Create a new User
CREATE USER 'spaigh'@'localhost' IDENTIFIED BY 'Spaigh'; // Note that "Spaigh" is the password of our user
                                                         // You can enter your own password instead, but be careful 
                                                         // and make sure to remember it 

//Then run each of the two lines below
GRANT ALL ON spaigh_database.* TO `spaigh`@'localhost'; 

//Then exit 
exit

//After `exit`, run the below in your terminal
mysql -u spaigh -p  // you'll be prompted for a password
                    // enter the password you set earlier when running `sudo mysql_secure_installation`

//See your databases (you should see the database name `spaigh_database` in the list). If not, you might have to recreate the database
//restarting from the //First create a database step.
show databases; 

//Then create a table as shown below
create table spaigh_database.spaigh_table(time_stamp VARCHAR(100), device_state VARCHAR(100), call_state VARCHAR(100), primary key(time_stamp));

//Now exit and we're done with the database setup!
exit

```

---
## Server side scripting using PHP
---
Create a PHP script in your `/var/www/spaigh` folder.

Name the PHP script `spaigh.php`

Now open `spaigh.php` with your preferred code editor. Note that you need **sudo** permission to create
and edit this file.

Follow the below steps to achieve the complete the above instructions: 

Run the below in a terminal: 

```
sudo nano /var/www/spaigh/spaigh.php

```

Copy the below PHP code into the file and save (use ctrl+shift+v to paste in nano,
                                                use ctrl+x to save
                                                type y to the prompt
                                                then press enter):

```
<?php
$user_name = "spaigh";
$password = "Spaigh"; // if you did not use "Spaigh" as your password, enter the password you created
$server = "localhost";
$db_name = "spaigh_database";
$table_name = "spaigh_table";

$con = mysqli_connect($server, $user_name, $password, $db_name);

if($con){
        $time_stamp = $_POST['time_stamp'];
        $device_state = $_POST['device_state'];
        $call_state = $_POST['call_state'];

        $query = "INSERT INTO $table_name VALUES ('".$time_stamp."', '".$device_state."', '".$call_state."');";
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
## Checking Data
---
In order to check the data stored in you MySQL database, 

Enter the following in a terminal: 

```
mysql -u spaigh -p  //enter your password ("Spaigh" is the default password if you did not change it)

//Then type in the following command in the mysql shell
select * from spaigh_database.spaigh_table; 

```

---
## Setting up the application
---
* Install and run the application

* Make sure that your **phone** and **server** are connected to the **same** LAN

* Get the server's IP address by running `ifconfig` command on a terminal in the server 

* If you are using a Wi-Fi network, check the number after the `inet` parameter under `wlo1` for you IP address

* If you are using an ethernet wired connection instead, check the number after the `inet` parameter under `eno1` for you IP address

* Now, after opening the app, in the text box hinting **server address**, type in `http://your_server_ip_address/spaigh.php`

* Note that you might have to add some additional endpoints to the URL above depending on where your PHP script is located
relative to your localhost director

* Now, click the `start service` button to start the app service.

* Accept the allow to use `Phone` permission

* You can close the app and the service will run in the foreground with a notification at the top of your screen

* In case you want to stop the service, open the app again and click the `stop service` button. 

* And that's it, check your database to see the collected data. 

---
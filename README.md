
<html>
  <h1>Supplies Tracker</h1><br>
  
  
  
Tracks supplies (food, potions, teleports, ect.), Ammo (cannonballs included), runes, and weapons charges.

Farming supplies now tracked in update 1.7

To turn on weapon charges box (tracker will no longer add each individual item used for a charge), look in configs.

Blowpipe ammo used is not actual use. It is an estimate based on number of attacks.

#### Weapons currently implemented ####

* Scythe
* Sanguinesti Staff
* Trident of the swamps
* Trident of the seas
* Blade of Saeldor
* Tumeken's Shadow

//TODO

Crystal tools <br>

</html>
*preview has weapon charges enabled and disabled to show both styles*

![Preview](https://i.gyazo.com/dc858c9708d3da4eb2f5fdcc73d424b5.png)


## Ability to save as JSON 
Button on UI will allow user to swap to use JSON. 

All previous entries will not have a date associated with it, as it is not possible to know. Anything that happened in the session may get the current time and date. 
This will also save differently into JSON files. Each object in the JSON file will be:

Items:
	Item:
		Name:
		Consumed_time
		

		
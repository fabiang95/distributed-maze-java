# distributed_maze

Compile (in IntelliJ command+F9)

# Init RMI Registery

<code>cd out/production/distributed_maze
rmiregistry</code>


  
  ## Bind the Tracker to RMI Registry (in seperate terminal)

  <code>cd out/production/distributed_maze 
  java tracker.Tracker <portnumber> 15 10</code>

  use some port number (like 6789)

## Start node  (in separate terminal)

  <code>cd out/production/distributed_maze
  java players.Node localhost <portnumber> P1</code>

use the same port number as the tracker 

## Start other nodes  (each in its separate terminal)

  <code>cd out/production/distributed_maze
  java players.Node localhost <portnumber> PN</code>
  
  replace N with node number

## Running Stress Test

start rmi registry and tracker following above

copy StressTest.java into out/production/DistributedMaze folder

go to the folder and run <code>
  java StressTest.java localhost <portnumber> "java Game"</code>

  portnumber needs to be the same as tracker port

  
  
## Application Class Diagram

https://app.diagrams.net/#G1mFUQJO_0NiDj6xbQO-7Ex_qJY-Z041jt


## rough logic for handling failed communication cases:
[distributed maze error handling logic notion](https://stump-milkshake-736.notion.site/Assignment-1-Distributed-Maze-5cafffd99e8b43bea2c209091ffad547?pvs=4)






# DES_UAV
A discrete event simulation of teams of UAV robotic drones moving through a system.  This was a project for a class that I built in my processing-based UI framework.

Basically the drone teams move from task to task, completing them in a certain amount of time before they move on.  The tasks themselves come in two types, single-service resources (where only a single team can perform the task at a time) and multi-service resources (where any number of teams can perform the task.)

The linkages between each task are called "resource-queues" and represent the the distance between each task along with a FIFO queue for the UAVs wait to enter occupied tasks.  

All state changes in the system are driven by events, which are in turned managed by a Future Event List, which is a priority queue, prioritizing on arrival time.  As part of the assignment' requirements, I implemented both a minheap-based and maxheap-based PQ.  Testing methods to guarantee the integrity and capabilities of the queue are built in and accessible via UI interations, as are tests of the simulation itself.  

Due to how I built the code, it is easy to procudrally generate large simualtion worlds, since each task (resource) and transit lane (resource queue) only interact with their immediate neighbors.  When multiple queues feed a single resource, the queue containing the lowest timestamp is popped from, while for resources that in turn feed multiple queues, the choice between possible queues is determined uniformly among them (this can also be easily modified to weight certain future resources across all UAVs or have certain tasks weighted differently for each team of UAVs).

Here are some videos of the system in action - these first 3 were of the simualtion working on the system under study : 

https://www.dropbox.com/s/v5xhdpih0hxanop/DESsim1.mp4?dl=0

https://www.dropbox.com/s/aytggophabqyvs1/DESsim2.mp4?dl=0

https://www.dropbox.com/s/salmgowp0c60v51/DESsim3.mp4?dl=0

this last video shows a very large procedurally-generated system in action.

https://www.dropbox.com/s/9qq6ee0d0mhq02j/DES_BigMap1.mp4?dl=0

EXECUTION OF SCHEDULER FRAMEWORK

1. As Eclipse project
	1.1. Unzip SchedulerFramework.zip
	1.2. Import Eclipse project from root folder SchedulerFramework
	1.3. Open src/scheduler/Main.java
	1.4. Go to Run -> Run Configurations
	1.5. Select Arguments and add <dotfile> <resource_constraints_file> to Program arguments
		(e.g. <dotfile> = graphs/testCyclic.dot
			  <resource_constraints_file> = resources/homogenous_16pe)


2. With make
	2.1. Unzip SchedulerFramework.zip
	2.2. Change directory to <path>/SchedulerFramework
	2.3. Call: 
			make run args="<dotfile> <resource_constraints_file>"
		(e.g. <dotfile> = graphs/testCyclic.dot
			  <resource_constraints_file> = resources/homogenous_16pe)

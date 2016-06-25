"# N98" 

This code implemented the TCA model in Nagel, K., Wolf, D. E., Wagner, P., & Simon, P. (1998). Two-lane traffic rules for cellular automata: A systematic approach. Physical Review E, 58(2), 1425–1437. http://doi.org/10.1103/PhysRevE.58.1425

You can verify 3 situations mentioned in the paper:
- section VI, ABC, fig2 in the paper
- section VIII, A, fig4 in the paper
- section VIII, D, fig7 in the paper

And you may run the model in other situations by tuning parameters.

The main point of the project is TrafficSimulation.main(). You could verify the model by setting TEST_MODEL = true and go to the TrafficSimulation.testModel() to comment out and uncomment the corresponding situations and tune parameters.

OR, you may set TEST_MODEL = false and go to the TrafficSimulation.getStatisticalData() to tune parameters and generate statistical data which are then saved in the file simulations.csv at the root directory of the project.

Have fun!

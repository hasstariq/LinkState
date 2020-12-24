import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class LinkState {
    private static ArrayList<Router> initialRouters = new ArrayList<>();
    public static void main(String[] args) {
        //runFromFile();
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();

       initialRouters = getNodesFromInput(n);

        for (int id:getGatewayRouters()) {
            System.out.println(createRoutingTable(id));
        }
    }
    private static ArrayList<Router> getNodesFromInput(int n){
        Scanner scanner = new Scanner(System.in);
        //Initialise routers
        ArrayList<Router> routers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            routers.add(new Router(i+1));
        }
        //Set distances between routers
        for (int i = 0; i < n; i++) {
            String[] distances = scanner.nextLine().split(" ");
            for (int j = 0; j < n; j++) {
                int distance = Integer.parseInt(distances[j]);
                if (distance != -1){
                    routers.get(i).addDestination(routers.get(j),distance);
                }else {
                    routers.get(i).addDestination(routers.get(j),Integer.MAX_VALUE);
                }
            }
        }
        return routers;
    }
    private static int[] getGatewayRouters(){
        Scanner scanner = new Scanner(System.in);
        String[] ids = scanner.nextLine().split(" ");
        return Arrays.stream(ids).mapToInt(Integer::parseInt).toArray();
    }

    private static void runFromFile(){
        BufferedReader reader;
        try {

            reader = new BufferedReader(new FileReader(
                    "src/sample.txt"));
            String line = reader.readLine();
            int countTo = Integer.parseInt(line);
            ArrayList<Router> routers = new ArrayList<>();
            for (int i = 0; i < countTo; i++) {
                routers.add(new Router(i+1));
            }
            for (int i = 0; i < countTo; i++) {
                line = reader.readLine();
                String[] distances = line.split(" ");
                for (int j = 0; j < countTo; j++) {
                    int distance = Integer.parseInt(distances[j]);
                    if (distance != -1){
                        routers.get(i).addDestination(routers.get(j),distance);
                    }else {
                        routers.get(i).addDestination(routers.get(j),Integer.MAX_VALUE);
                    }
                }
            }

            initialRouters = routers;
            line = reader.readLine();
            reader.close();
            String[] ids = line.split(" ");
            for (String id: ids) {
                System.out.println(createRoutingTable(Integer.parseInt(id)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void calculateShortestPathFromSource(Router source) {
        //Initializing cost to 0
        source.setDistance(0);
        //routers that we have visited
        Set<Router> settledRouters = new HashSet<>();
        //routers that we need to check
        Set<Router> unsettledRouters = new HashSet<>();
        unsettledRouters.add(source);

        while (unsettledRouters.size() != 0) {
            //get closest router
            Router currentRouter = getLowestDistanceRouter(unsettledRouters);
            unsettledRouters.remove(currentRouter);
            //Iterate over all neighbors
            for (Map.Entry<Router, Integer> adjacencyPair : currentRouter.getAdjacentRouters().entrySet()) {
                Router neighbor = adjacencyPair.getKey();
                Integer costToNeighbor = adjacencyPair.getValue();
                //Check if we already visited this neighbor
                if (!settledRouters.contains(neighbor)) {
                    calculateDistanceToRouter(neighbor, costToNeighbor, currentRouter);
                    unsettledRouters.add(neighbor);
                }
            }
            settledRouters.add(currentRouter);
        }

    }

    private static void calculateDistanceToRouter(Router neighbor, Integer costToNeighbor, Router currentRouter) {
        Integer sourceDistance = currentRouter.getDistance();
        int actualDistance = sourceDistance+costToNeighbor;
        //If distance is smaller or overflows set huge cost
        if (actualDistance < 0){
            actualDistance = Integer.MAX_VALUE;
        }
        if (actualDistance < neighbor.getDistance()) {
            neighbor.setDistance(actualDistance);
            LinkedList<Router> shortestPath = new LinkedList<>(currentRouter.getShortestPath());
            shortestPath.add(currentRouter);
            neighbor.setShortestPath(shortestPath);
        }
    }
    //returns the router with the lowest distance
    private static Router getLowestDistanceRouter(Set<Router> unsettledRouters) {
        return unsettledRouters.stream().min(Comparator.comparing(Router::getDistance)).get();

        /*Router lowestDistanceRouter = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (Router router : unsettledRouters) {
            int distance = router.getDistance();
            if (distance < lowestDistance) {
                lowestDistance = distance;
                lowestDistanceRouter = router;
            }
        }
        return lowestDistanceRouter;*/
    }

    /*We see an alternative route as a route that has a different hop instead of a different route later down
    Example: Route from A to F with cost 3
    We will see this as alternative hop
            A--1-->B--1-->D--1-->F
            A--1-->C--1-->E--1-->F

    But not this
            A--1-->B--1-->D--1-->F
            A--1-->B--1-->E--1-->F
    */
    private static List<Router> findAlternativeRoute(Router source, int destinationId,int initialCost, List<Router> initialRoute){
        for (Map.Entry<Router, Integer> adjacentRouter: source.getAdjacentRouters().entrySet()){
            List<Router> routers = getInitialRouters();
            Router start = routers.stream().filter(e -> e.equals(source)).findFirst().get();
            Router current = routers.stream().filter(e -> e.equals(adjacentRouter.getKey())).findFirst().get();
            if (current.getName() != source.getName()){
                Router dest = routers.stream().filter(e -> e.getName() == destinationId).findFirst().get();
                calculateShortestPathFromSource(current);
                int cost = adjacentRouter.getValue() + dest.getDistance();
                LinkedList<Router> altPath = (LinkedList<Router>) dest.getShortestPath();
                altPath.addLast(dest);
                altPath.addFirst(start);
                //Cost must be equal (we will not find better) && There should be a path && The path is a new path
                if (cost == initialCost && dest.getShortestPath().size() != 0 && !Arrays.equals(initialRoute.toArray(), altPath.toArray())){
                    //Returns the alternative path
                    return dest.getShortestPath();
                }
            }
        }
        //no alternative found
        return null;
    }

    private static String createRoutingTable(int sourceId) {
        //Table format
        String format = "%10s%10s%10s\n";
        ArrayList<Router> routers = getInitialRouters();
        Router source = routers.get(sourceId - 1);
        calculateShortestPathFromSource(source);
        StringBuilder table = new StringBuilder("Forwarding Table for " + sourceId + "\n");
        table.append(String.format(format, "To", "Cost", "Next Hop"));
        for (int i = 0; i < routers.size(); i++) {
            String id = Integer.toString(i + 1);
            if (i + 1 != sourceId) {
                //Get the current node and get the path from source-current
                Router to = routers.get(i);
                List<Router> path = to.getShortestPath();
                path.add(to);
                //the cost of the path
                int cost = to.getDistance();
                String hop;
                try {
                    hop = Integer.toString(path.get(1).getName());
                    List<Router> altPath =  findAlternativeRoute(source, to.getName(), cost, to.getShortestPath());
                    if (altPath != null){
                        hop += " " +  altPath.get(1).getName();
                    }
                }catch (Exception ignored){
                    //Exception gets thrown because we have not found a path so we set hop and cost to -1
                    hop = "-1";
                    cost = -1;
                }
                table.append(String.format(format,id, cost,hop));
            }
        }
        return table.toString();
    }

    /*
     * Copies and returns the initial state
     */
    private static ArrayList<Router> getInitialRouters(){
        ArrayList<Router> toReturn = new ArrayList<>();
        initialRouters.forEach(router -> {
            Router cloned = new Router(router.getName());
            toReturn.add(cloned);
        });
        toReturn.forEach(router -> {
            Router initialRouter = initialRouters.get(router.getName()-1);
            for (Map.Entry<Router, Integer> pair : initialRouter.getAdjacentRouters().entrySet()) {
                int index = pair.getKey().getName()-1;
                router.addDestination(toReturn.get(index), pair.getValue());
            }
        });
        return toReturn;
    }

    /*
     * Data class that will hold references and values
     */
    public static class Router {

        private int id;

        private LinkedList<Router> shortestPath = new LinkedList<>();

        private Integer distance = Integer.MAX_VALUE;

        private Map<Router, Integer> adjacentRouters = new HashMap<>();

        public Router(int id) {
            this.id = id;
        }

        public void addDestination(Router destination, int distance) {
            adjacentRouters.put(destination, distance);
        }

        public int getName() {
            return id;
        }


        public Map<Router, Integer> getAdjacentRouters() {
            return adjacentRouters;
        }

        public void setAdjacentRouters(Map<Router, Integer> adjacentRouters) {
            this.adjacentRouters = adjacentRouters;
        }

        public Integer getDistance() {
            return distance;
        }

        public void setDistance(Integer distance) {
            this.distance = distance;
        }

        public List<Router> getShortestPath() {
            return shortestPath;
        }

        public void setShortestPath(LinkedList<Router> shortestPath) {
            this.shortestPath = shortestPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Router router = (Router) o;
            return id == router.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }


    }
}

export function addRouteBoxToGraph(graph, routeBoxes, routeId, box) {
    graph.addCell(box);
    routeBoxes[routeId] = box;
}
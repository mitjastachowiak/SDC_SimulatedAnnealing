package scheduler;

public enum RT {
		MEM (2, 9.0, "Mem"),
		ADD (1, 1.0, "Add"),
		SUB (1, 1.4, "Sub"),
		MUL (4, 2.3, "Mul"),
		DIV (18, 4.3, "Div"),
		SH (1, 2.0, "Shift"),
		AND (1, 2.0, "And"),
		OR (1, 2.0, "Or"),
		CMP (1, 2.1, "Cmp"),
		OTHER (1, 1.0, "Other"),
		SLACK(1, 0.0, "Slack");
	
	/**
	 * Delay (duration) of this resource type
	 */
	public final Integer delay;
	
	/**
	 * Weight of this resource type
	 */
	public final Double weight;
	
	/**
	 * Name of this resource type
	 */
	public final String name;
	
	private RT(Integer delay, Double weight, String name) {
		this.delay = delay;
		this.weight = weight;
		this.name = name;
	}
	
	/**
	 * Get the resource type for the given string. 
	 * @param id - the ID of the node to get the resource type from
	 * @return the resource type. RT.OTHER if none was found
	 */
	public static RT getRT(String id){
		if (id.contains("MUL")) {
			return RT.MUL;
		} else if (id.contains("ADD")|| id.contains("INC")) {
			return RT.ADD;
		} else if (id.contains("DIV")) {
			return RT.DIV;
		} else if (id.contains("SUB")) {
			return RT.SUB;
		} else if (id.contains("SH")) {
			return RT.SH;
		} else if (id.contains("AND")) {
			return RT.AND;
		} else if (id.contains("MEM")) {
			return RT.MEM;
		} else if (id.contains("STORE") || id.contains("LOAD")) {
			return RT.MEM;
		} else if (id.contains("OR")) {
			return RT.OR;
		} else if (id.contains("IF") || id.contains("CMP")) {
			return RT.CMP;
		} else {
			return RT.OTHER;
		}
	}
}

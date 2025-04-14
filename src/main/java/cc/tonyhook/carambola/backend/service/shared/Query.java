package cc.tonyhook.carambola.backend.service.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Query {
    public Map<String, List<String>> filter;

    public List<String> searchKey;

    public String searchValue;

    public Query() {
        filter = new HashMap<String, List<String>>();
        searchKey = new ArrayList<String>();
        searchValue = "";
    }

}

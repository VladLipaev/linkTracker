package backend.academy.linktracker.scrapper.repository.utils;

import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.List;

public class ParseSortUtil {

    public static Sort parseSort(String sort){
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        String[] parts = sort.split(",");
        List<Sort.Order> orders = new ArrayList<>();

        for (int i = 0; i < parts.length; i += 2) {
            String field = parts[i].trim();
            Sort.Direction direction = (i + 1 < parts.length)
                ? Sort.Direction.fromString(parts[i + 1].trim())
                : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, field));
        }

        return Sort.by(orders);
    }

}

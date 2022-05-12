package edu.berkeley.cs.jqf.fuzz.rl;

import java.util.List;

/**
 * Created by clemieux on 6/17/19.
 */
public interface Guide {
    Object select(List<Object> actions, String state, int id);
//     <T> T Select(List<T> actions, String state, int id);

}

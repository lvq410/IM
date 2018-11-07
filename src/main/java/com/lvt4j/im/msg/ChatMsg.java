package com.lvt4j.im.msg;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author LV
 */
@Data
@AllArgsConstructor(staticName="of")
public class ChatMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String from;
    public final String to;
    public final String content;

}

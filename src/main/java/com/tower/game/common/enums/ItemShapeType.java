package com.tower.game.common.enums;

import lombok.Getter;

/**
 * 道具背包占格形态（9 种）
 */
@Getter
public enum ItemShapeType {
    SHAPE_1x1(1, 1, 1),
    SHAPE_1x2(2, 1, 2),
    SHAPE_2x1(3, 2, 1),
    SHAPE_2x2(4, 2, 2),
    SHAPE_1x3(5, 1, 3),
    SHAPE_3x1(6, 3, 1),
    SHAPE_2x3(7, 2, 3),
    SHAPE_3x2(8, 3, 2),
    SHAPE_3x3(9, 3, 3);

    private final int code;
    private final int rows;
    private final int cols;

    ItemShapeType(int code, int rows, int cols) {
        this.code = code;
        this.rows = rows;
        this.cols = cols;
    }

    public static ItemShapeType fromCode(int code) {
        for (ItemShapeType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }
}

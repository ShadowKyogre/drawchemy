/*
 * This file is part of the Drawchemy project - https://code.google.com/p/drawchemy/
 *
 * Copyright (c) 2014 Pilmeyer Patrick
 *
 * Drawchemy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Drawchemy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Drawchemy.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.google.code.drawchemy.creator;

import com.google.code.drawchemy.DrawManager;

public abstract class ACreator {

    protected final DrawManager fManager;

    public ACreator(DrawManager aManager) {
        fManager = aManager;
    }

    public abstract IDrawingOperation startDrawingOperation(float x, float y);

    public abstract void updateDrawingOperation(float x, float y);

    public abstract void endDrawingOperation();
}

package dev.ricr.skyblock.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Tuple<T, U> {

    public T first;
    public U second;

}

package com.testing.Java8Features;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
public class ExampleDto {
    public final Integer integer;
    public final Boolean bool;
    @Builder.Default
    public final List<String> strings = new ArrayList<>();
}

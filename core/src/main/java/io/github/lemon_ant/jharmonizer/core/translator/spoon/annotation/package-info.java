// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

/**
 * Source annotation parsing and metadata preparation performed by the translator before sorting.
 * {@link io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSourceScanner} is the entry point;
 * its package-private helpers resolve names, repeatable containers, and conservative ordering dependencies.
 * Type-name formatting, import preparation, and inherited member-type lookup stay here because their production
 * consumers are these annotation processors. Parser-wide type-tree traversal belongs to the parent package.
 * {@link AnnotationSrcGroup} stays beside the scanner to restrict initial group and fragment construction to this
 * package. The shared model carries prepared fragments to the sorter and printer; public copy operations retain
 * source ranges while accepting state already selected by the sorter. Ordering and source serialization remain
 * in their owning processing stages.
 */
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

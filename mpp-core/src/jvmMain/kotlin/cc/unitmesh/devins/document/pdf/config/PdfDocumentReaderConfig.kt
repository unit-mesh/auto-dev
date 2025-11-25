/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.unitmesh.devins.document.pdf.config

/**
 * Common configuration builder for the PagePdfDocumentReader and the
 * ParagraphPdfDocumentReader.
 *
 * @author Christian Tzolov
 */
data class PdfDocumentReaderConfig(
    val reversedParagraphPosition: Boolean = false,
    val pagesPerDocument: Int = 1,
    val pageTopMargin: Int = 0,
    val pageBottomMargin: Int = 0,
    val pageExtractedTextFormatter: (String, Int) -> String = { text, _ -> text }
) {
    companion object {
        const val ALL_PAGES = 0

        /**
         * Start building a new configuration.
         * @return The entry point for creating a new configuration.
         */
        fun builder(): Builder {
            return Builder()
        }

        /**
         * @return the default config
         */
        fun defaultConfig(): PdfDocumentReaderConfig {
            return builder().build()
        }
    }

    class Builder {
        private var pagesPerDocument: Int = 1
        private var pageTopMargin: Int = 0
        private var pageBottomMargin: Int = 0
        private var pageExtractedTextFormatter: (String, Int) -> String = { text, _ -> text }
        private var reversedParagraphPosition: Boolean = false

        /**
         * Formatter of the extracted text.
         * @param pageExtractedTextFormatter Instance of the PageExtractedTextFormatter.
         * @return this builder
         */
        fun withPageExtractedTextFormatter(
            pageExtractedTextFormatter: (String, Int) -> String
        ): Builder {
            this.pageExtractedTextFormatter = pageExtractedTextFormatter
            return this
        }

        /**
         * How many pages to put in a single Document instance. 0 stands for all pages.
         * Defaults to 1.
         * @param pagesPerDocument Number of page's content to group in single Document.
         * @return this builder
         */
        fun withPagesPerDocument(pagesPerDocument: Int): Builder {
            require(pagesPerDocument >= 0) { "Page count must be a positive value." }
            this.pagesPerDocument = pagesPerDocument
            return this
        }

        /**
         * Configures the Pdf reader page top margin. Defaults to 0.
         * @param topMargin page top margin to use
         * @return this builder
         */
        fun withPageTopMargin(topMargin: Int): Builder {
            require(topMargin >= 0) { "Page margins must be a positive value." }
            this.pageTopMargin = topMargin
            return this
        }

        /**
         * Configures the Pdf reader page bottom margin. Defaults to 0.
         * @param bottomMargin page top margin to use
         * @return this builder
         */
        fun withPageBottomMargin(bottomMargin: Int): Builder {
            require(bottomMargin >= 0) { "Page margins must be a positive value." }
            this.pageBottomMargin = bottomMargin
            return this
        }

        /**
         * Configures the Pdf reader reverse paragraph position. Defaults to false.
         * @param reversedParagraphPosition to reverse or not the paragraph position
         * within a page.
         * @return this builder
         */
        fun withReversedParagraphPosition(reversedParagraphPosition: Boolean): Builder {
            this.reversedParagraphPosition = reversedParagraphPosition
            return this
        }

        /**
         * @return the immutable configuration
         */
        fun build(): PdfDocumentReaderConfig {
            return PdfDocumentReaderConfig(
                reversedParagraphPosition = reversedParagraphPosition,
                pagesPerDocument = pagesPerDocument,
                pageTopMargin = pageTopMargin,
                pageBottomMargin = pageBottomMargin,
                pageExtractedTextFormatter = pageExtractedTextFormatter
            )
        }
    }
}

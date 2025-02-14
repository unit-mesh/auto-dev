/**
 * Copyright 2023 Continue Dev, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.unitmesh.devti.diff

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor

@Service(Service.Level.PROJECT)
class DiffStreamService {
    private val handlers = mutableMapOf<Editor, DiffStreamHandler>()

    fun register(handler: DiffStreamHandler, editor: Editor) {
        if (handlers.containsKey(editor)) {
            handlers[editor]?.rejectAll()
        }
        handlers[editor] = handler
    }

    fun reject(editor: Editor) {
        handlers[editor]?.rejectAll()
        handlers.remove(editor)
    }

    fun accept(editor: Editor) {
        handlers[editor]?.acceptAll()
        handlers.remove(editor)
    }
}
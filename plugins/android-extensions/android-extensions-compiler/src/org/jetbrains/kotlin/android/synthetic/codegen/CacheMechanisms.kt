/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.synthetic.codegen

import kotlinx.android.extensions.AndroidEntityOptions
import kotlinx.android.extensions.CacheImplementation
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidEntityOptionsProxy
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface CacheMechanism {
    /** Push the cache object onto the stack. */
    fun loadCache()

    /** Init cache variable. */
    fun initCache()

    /** Push the cached view onto the stack, or push `null` if the view is not cached. `Int` id should be on the stack. */
    fun getViewFromCache()

    /** Cache the view. `Int` id should be on the stack. */
    fun putViewToCache(getView: () -> Unit)

    companion object {
        fun get(entityOptions: AndroidEntityOptionsProxy, iv: InstructionAdapter, containerType: Type): CacheMechanism {
            return when (entityOptions.cache) {
                CacheImplementation.HASH_MAP -> HashMapCacheMechanism(iv, containerType)
                CacheImplementation.NO_CACHE -> throw IllegalArgumentException("Container should support cache")
            }
        }
    }
}

internal class HashMapCacheMechanism(
        val iv: InstructionAdapter,
        val containerType: Type
) : CacheMechanism {
    override fun loadCache() {
        iv.load(0, containerType)
        iv.getfield(containerType.internalName, AndroidExpressionCodegenExtension.PROPERTY_NAME, "Ljava/util/HashMap;")
    }

    override fun initCache() {
        iv.load(0, containerType)
        iv.anew(Type.getType("Ljava/util/HashMap;"))
        iv.dup()
        iv.invokespecial("java/util/HashMap", "<init>", "()V", false)
        iv.putfield(containerType.internalName, AndroidExpressionCodegenExtension.PROPERTY_NAME, "Ljava/util/HashMap;")
    }

    override fun getViewFromCache() {
        iv.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        iv.invokevirtual("java/util/HashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false)
    }

    override fun putViewToCache(getView: () -> Unit) {
        iv.invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        getView()
        iv.invokevirtual("java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
        iv.pop()
    }
}
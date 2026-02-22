package org.example.encryptmodule

import org.example.encryptmodule.domain.Member
import org.example.encryptmodule.domain.MemberRepository
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.Optional
import java.util.function.Function

/**
 * 테스트용 인메모리 MemberRepository
 *
 * 서비스 로직(비즈니스 규칙) 검증 목적.
 * JPA를 거치지 않으므로 @EntityListeners / @Convert 는 동작하지 않음.
 * → 암복호화 검증은 MemberRepositoryDataJpaTest(@DataJpaTest) 에서 수행.
 *
 * save() 에서 nameEncrypted / phoneNumberEncrypted 를 직접 세팅해
 * EntityListener 없이도 getMembers() 가 정상 동작하도록 처리.
 */
class FakeMemberRepository : MemberRepository {

    private val store = mutableMapOf<Long, Member>()
    private var idSeq = 1L

    // ── 핵심 구현 ──────────────────────────────────────────────

    override fun <S : Member> save(entity: S): S {
        if (entity.id == null) entity.id = idSeq++
        // JPA @PrePersist/@PreUpdate 없이 암호화 필드 직접 세팅
        entity.nameEncrypted = entity.name
        entity.phoneNumberEncrypted = entity.phoneNumber
        store[entity.id!!] = entity
        return entity
    }

    override fun findById(id: Long): Optional<Member> = Optional.ofNullable(store[id])

    override fun existsById(id: Long): Boolean = store.containsKey(id)

    override fun deleteById(id: Long) { store.remove(id) }

    override fun findMembersByIdIn(ids: List<Long>): List<Member> =
        store.values.filter { it.id in ids }

    // ── JpaRepository stubs ────────────────────────────────────

    override fun findAll(): MutableList<Member> = store.values.toMutableList()

    override fun findAll(sort: Sort): MutableList<Member> = findAll()

    override fun findAll(pageable: Pageable): Page<Member> = PageImpl(findAll())

    override fun findAllById(ids: Iterable<Long>): MutableList<Member> =
        store.values.filter { it.id in ids }.toMutableList()

    override fun <S : Member> saveAll(entities: Iterable<S>): MutableList<S> =
        entities.map { save(it) }.toMutableList()

    override fun count(): Long = store.size.toLong()

    override fun delete(entity: Member) { store.remove(entity.id) }

    override fun deleteAll() { store.clear() }

    override fun deleteAll(entities: Iterable<Member>) { entities.forEach { delete(it) } }

    override fun deleteAllById(ids: Iterable<Long>) { ids.forEach { deleteById(it) } }

    override fun flush() {}

    override fun <S : Member> saveAndFlush(entity: S): S = save(entity)

    override fun <S : Member> saveAllAndFlush(entities: Iterable<S>): MutableList<S> = saveAll(entities)

    override fun deleteAllInBatch(entities: Iterable<Member>) = deleteAll(entities)

    override fun deleteAllByIdInBatch(ids: Iterable<Long>) = deleteAllById(ids)

    override fun deleteAllInBatch() = deleteAll()

    override fun getReferenceById(id: Long): Member =
        findById(id).orElseThrow { NoSuchElementException("Member not found: $id") }

    @Deprecated("Use getReferenceById", ReplaceWith("getReferenceById(id)"))
    override fun getOne(id: Long): Member = getReferenceById(id)

    @Deprecated("Use getReferenceById", ReplaceWith("getReferenceById(id)"))
    override fun getById(id: Long): Member = getReferenceById(id)

    override fun <S : Member> findAll(example: Example<S>): MutableList<S> = TODO()

    override fun <S : Member> findAll(example: Example<S>, sort: Sort): MutableList<S> = TODO()

    override fun <S : Member> findAll(example: Example<S>, pageable: Pageable): Page<S> = TODO()

    override fun <S : Member> findOne(example: Example<S>): Optional<S> = TODO()

    override fun <S : Member> count(example: Example<S>): Long = TODO()

    override fun <S : Member> exists(example: Example<S>): Boolean = TODO()

    override fun <S : Member, R> findBy(
        example: Example<S>,
        queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>
    ): R = TODO()
}

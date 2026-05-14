package dev.ktcloud.black.auth.application.service.jwt

import dev.ktcloud.black.user.domain.entity.UserDomainEntity
import dev.ktcloud.black.user.domain.vo.UserRole
import io.jsonwebtoken.JwtException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * R-57 (평가 기본 (3)-1): JwtGenerator + JwtResolver round-trip 단위 테스트.
 *
 * @Component 라도 생성자 인자가 모두 단순 String 이라 Spring context 없이 직접 인스턴스화
 * 가능. round-trip — generate 한 토큰을 validateToken 으로 다시 parse 해서 동일 user
 * 정보 복원되는지 검증.
 *
 * 본 테스트가 통과하면 평가 기본 (2)-2 "JWT 인증 필터" 의 핵심 동작이 정상.
 */
@DisplayName("Jwt Generator/Resolver - 발급 + 검증 round-trip")
class JwtRoundTripTest {

    // HS256 사용 시 256bit(32byte) 이상 길이 필요 (jjwt 0.12 정책).
    private val secret = "test-secret-key-32-bytes-minimum-length-padding-padding"

    private val generator = JwtGenerator(secret)
    private val resolver = JwtResolver(secret)

    private fun user(role: UserRole = UserRole.USER) = UserDomainEntity(
        role = role,
        email = "user@troica.dev",
        password = "hashed",
        name = "테스트 사용자",
    )

    @Test
    @DisplayName("generate 결과는 access / refresh 두 토큰의 Pair")
    fun `generate 는 두 토큰 반환`() {
        val (access, refresh) = generator.generate(user())

        assertThat(access).isNotBlank()
        assertThat(refresh).isNotBlank()
        // 두 토큰은 다른 expiration → 다른 payload → 다른 문자열
        assertThat(access).isNotEqualTo(refresh)
    }

    @Test
    @DisplayName("access token 을 validateToken 에 넣으면 동일한 user 정보 복원")
    fun `발급 - 검증 round-trip`() {
        val u = user(UserRole.ADMIN)
        val (access, _) = generator.generate(u)

        val resolved = resolver.validateToken(access)

        assertThat(resolved.id).isEqualTo(u.id)
        assertThat(resolved.role).isEqualTo(UserRole.ADMIN)
        assertThat(resolved.email).isEqualTo(u.email)
        assertThat(resolved.name).isEqualTo(u.name)
    }

    @Test
    @DisplayName("다른 secret 으로 만든 resolver 는 검증 실패 (서명 불일치)")
    fun `secret 불일치 시 JwtException`() {
        val (access, _) = generator.generate(user())

        val foreignResolver = JwtResolver("different-secret-key-32-bytes-minimum-padding-padding!")

        assertThatThrownBy { foreignResolver.validateToken(access) }
            .isInstanceOf(JwtException::class.java)
    }

    @Test
    @DisplayName("토큰 본문 변조 시 검증 실패 (서명 위배)")
    fun `토큰 변조 검출`() {
        val (access, _) = generator.generate(user())

        // 마지막 8자 변경 → 서명 위배
        val tampered = access.dropLast(8) + "AAAAAAAA"

        assertThatThrownBy { resolver.validateToken(tampered) }
            .isInstanceOf(JwtException::class.java)
    }

    @Test
    @DisplayName("extractClaims 가 subject / role / email / name 모두 보존")
    fun `claims 전체 보존`() {
        val u = user(UserRole.USER)
        val (access, _) = generator.generate(u)

        val claims = resolver.extractClaims(access)

        assertThat(claims.subject).isEqualTo(u.id.toString())
        assertThat(claims["role"]).isEqualTo(UserRole.USER.name)
        assertThat(claims["email"]).isEqualTo(u.email)
        assertThat(claims["name"]).isEqualTo(u.name)
    }
}

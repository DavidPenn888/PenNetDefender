//package com.pennet.defender.config;
//
//import com.pennet.defender.repository.UserRepository;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//
//    private final UserRepository userRepository;
//
////    @Override
////    protected void configure(HttpSecurity http) throws Exception {
////        http
////                .csrf().disable()
////                .authorizeRequests()
////                .antMatchers("/api/**", "/web/**", "/firewall/**").hasRole("ADMIN")
////                .anyRequest().authenticated()
////                .and()
////                .httpBasic();
////    }
//    public SecurityConfig(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @Bean
//    public UserDetailsService userDetailsService() {
//        return username -> userRepository.findByUsername(username)
//                .map(user -> org.springframework.security.core.userdetails.User
//                        .withUsername(user.getUsername())
//                        .password(user.getPassword())
//                        .roles("ADMIN")
//                        .build())
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//    }
//
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.inMemoryAuthentication()
//                .withUser("admin")
//                .password(passwordEncoder().encode("defender"))
//                .roles("ADMIN");
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}
package com.pennet.defender.config;

import com.pennet.defender.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 通过数据库加载用户信息
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles("ADMIN")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * 密码加密策略，使用 BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 5 之后，AuthenticationManager 需要手动暴露
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Spring Security 过滤器配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // 关闭 CSRF 保护（REST API 一般关闭，Web 应用建议开启）
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/", "/login", "/css/**", "/js/**").permitAll() // 允许所有用户访问登录页面及静态资源
//                        .antMatchers("/api/server/changepsw").authenticated() // 修改密码接口需要登录
                        .antMatchers("/dashboard", "/api/**" ).hasRole("ADMIN") // 只有管理员才能访问 /api 下的接口
                        .anyRequest().authenticated() // 其他所有请求需要认证
                )
                .formLogin(form -> form
                        .loginPage("/login") // 指定登录页面
                        .defaultSuccessUrl("/dashboard", true) // 登录成功后跳转到 /dashboard
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // 退出登录 URL
                        .logoutSuccessUrl("/login?logout") // 退出后跳转到登录页
                        .invalidateHttpSession(true) // 使 Session 失效
                        .deleteCookies("JSESSIONID") // 删除 Cookie
                );

        return http.build();
    }
}

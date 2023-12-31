package com.example.userservice.services;

import com.example.userservice.exceptions.NoOfActiveSessionExceeded;
import com.example.userservice.exceptions.TokenExpiredException;
import com.example.userservice.models.Session;
import com.example.userservice.repositories.SessionRepository;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.dtos.UserDto;
import com.example.userservice.models.SessionStatus;
import com.example.userservice.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMapAdapter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class AuthService {
    private UserRepository userRepository;
    private SessionRepository sessionRepository;

    private BCryptPasswordEncoder bCryptPasswordEncoder;



    public AuthService(UserRepository userRepository, SessionRepository sessionRepository,BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.bCryptPasswordEncoder=bCryptPasswordEncoder;
    }

    public ResponseEntity<UserDto> login(String email, String password) throws NoOfActiveSessionExceeded {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return null;
        } //return exception

        User user = userOptional.get();

        if (!bCryptPasswordEncoder.matches(password,user.getPassword())) {
            throw new RuntimeException("Wrong Password Entered"); // throw custom exception
        }

        if(sessionRepository.countByUser_IdAndSessionStatus(user.getId(),SessionStatus.ACTIVE) == 2)
        {
            throw new NoOfActiveSessionExceeded("No of Active session already 2");
        }

       // String token = RandomStringUtils.randomAlphanumeric(30); // JWT use
        // Create a test key suitable for the desired HMAC-SHA algorithm:
        MacAlgorithm alg = Jwts.SIG.HS256; //or HS384 or HS256
        SecretKey key = alg.key().build();

//        String message = "{\n" +
//                "\"email\": \"abhishek\",\n" +
//                "\"role\":[\"ADMIN\",\"USER\"]}";
        Map<String, Object> jsonMap=new HashMap<>();
        jsonMap.put("email",user.getEmail());
        jsonMap.put("roles", List.of(user.getRoles()));
        jsonMap.put("createdAt",new Date());
        jsonMap.put("expiryAt", DateUtils.addDays(new Date(),5));



       // byte[] content = message.getBytes(StandardCharsets.UTF_8);

        // Create the compact JWS: JSON WEB SIGNATURE
      //  String jws = Jwts.builder().content(content, "text/plain").signWith(key, alg).compact();

        String jws=Jwts.builder()
                .claims(jsonMap)
                .signWith(key,alg)
                .compact();
        // Parse the compact JWS:
       // content = Jwts.parser().verifyWith(key).build().parseSignedContent(jws).getPayload();

       // assert message.equals(new String(content, StandardCharsets.UTF_8));

        Session session = new Session();
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setToken(jws);
        session.setUser(user);
        LocalDateTime currentDateTime = LocalDateTime.now().plusMinutes(2);
        Date expiringAt = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant());
        session.setExpiringAt(expiringAt); // for expiry set
        sessionRepository.save(session);

        UserDto userDto = new UserDto();
        userDto.setEmail(email);

//        Map<String, String> headers = new HashMap<>();
//        headers.put(HttpHeaders.SET_COOKIE, token);

        MultiValueMapAdapter<String, String> headers = new MultiValueMapAdapter<>(new HashMap<>());
        headers.add(HttpHeaders.SET_COOKIE, "auth-token:" + jws);



        ResponseEntity<UserDto> response = new ResponseEntity<>(userDto, headers, HttpStatus.OK);
//        response.getHeaders().add(HttpHeaders.SET_COOKIE, token);

        return response;
    }

    public ResponseEntity<Void> logout(String token, Long userId) {
        Optional<Session> sessionOptional = sessionRepository.findByTokenAndUser_Id(token, userId);

        if (sessionOptional.isEmpty()) {
            return null;
        }

        Session session = sessionOptional.get();

        session.setSessionStatus(SessionStatus.ENDED);

        sessionRepository.save(session);

        return ResponseEntity.ok().build();
    }

    public UserDto signUp(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password)); // We should store the encrypted password in the DB for a user.
        
        User savedUser = userRepository.save(user);

        return UserDto.from(savedUser);
    }

    public SessionStatus validate(String token, Long userId) throws TokenExpiredException {
        Optional<Session> session = sessionRepository.findByTokenAndUser_Id(token, userId);

        if (session.isEmpty()) {
            return null;
        }

        if (session != null && session.get().getSessionStatus() == SessionStatus.ACTIVE) {
            Date expiringAt = session.get().getExpiringAt();
            Date now = new Date();

            if (now.after(expiringAt))
            {
                session.get().setSessionStatus(SessionStatus.ENDED);
                sessionRepository.save(session.get());

                throw new TokenExpiredException("Token has expired");
            }
            else
            {
                // Token is still valid
                // Proceed with the resource access
            }
        }
        else
        {
            // Token is not valid or session is not active
            // Handle accordingly
        }

        return SessionStatus.ACTIVE;
    }

}

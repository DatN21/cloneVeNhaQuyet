package com.dulich.toudulich.Service;

import com.dulich.toudulich.DTO.UserDTO;
import com.dulich.toudulich.DTO.UserDTOUpdate;
import com.dulich.toudulich.DTO.UserDTOUpdateByAdmin;
import com.dulich.toudulich.Model.RoleModel;
import com.dulich.toudulich.Model.UserModel;
import com.dulich.toudulich.Repositories.RoleRepository;
import com.dulich.toudulich.Repositories.UserRepository;
import com.dulich.toudulich.component.JwtTokenUtil;
import com.dulich.toudulich.exceptions.DataNotFoundException;
import com.dulich.toudulich.exceptions.InvalidParamException;
import com.dulich.toudulich.exceptions.PermissionDenyException;
import com.dulich.toudulich.exceptions.UnauthorizedException;
import com.dulich.toudulich.responses.BookingResponse;
import com.dulich.toudulich.responses.LoginResponse;
import com.dulich.toudulich.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements iUserService {
    private final UserRepository userRepository ;
    private final RoleRepository roleRepository ;
    private final PasswordEncoder passwordEncoder ;
    private final JwtTokenUtil jwtTokenUtil ;
    private final AuthenticationManager authenticationManager ;


    @Override
    public UserModel createUser(UserDTO userDTO) throws PermissionDenyException {
        String phoneNumber = userDTO.getPhone() ;
        if (userRepository.existsByPhone(phoneNumber)){
            throw new DataIntegrityViolationException("Phone number already exists");
        }
        RoleModel roleModel ;
        try {
            roleModel = roleRepository.findById(userDTO.getRoleID())
                    .orElseThrow(() -> new DataNotFoundException("Role not found"));
        } catch (DataNotFoundException e) {
            throw new RuntimeException("Role not found", e);  // Có thể ném ngoại lệ phù hợp với ngữ cảnh ứng dụng
        }
        if (roleModel.getRoleName().toUpperCase().equals(RoleModel.ADMIN)){
            throw new PermissionDenyException("You can not register an admin account") ;
        }

            String passWord = userDTO.getPassword();
          String encodePassword = passwordEncoder.encode(passWord);
        UserModel newUserModel = UserModel.builder().
                name(userDTO.getName())
                .phone(userDTO.getPhone())
                .gender(userDTO.getGender())
                .email(userDTO.getEmail())
                .address(userDTO.getAddress())
                .passWord(encodePassword)
                .build();
        newUserModel.setRoleId(roleModel);
      return  userRepository.save(newUserModel);
    }

    @Override
    public LoginResponse login(String phone, String password) throws DataNotFoundException, InvalidParamException {
        Optional<UserModel> userOptional =  userRepository.findByPhone(phone);
        if(userOptional.isEmpty()){
            throw new DataNotFoundException("Invalid phone number / password!");
        }
        //return userOptional.get();
        UserModel user = userOptional.get() ;
            if (!passwordEncoder.matches(password, user.getPassword())){
                throw new BadCredentialsException("Wrong phone number or password") ;
            }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(phone,password,user.getAuthorities()) ;
        authenticationManager.authenticate( authenticationToken);
        return LoginResponse.builder()
                .message("Login successful")
                .token(jwtTokenUtil.generateToken(user))
                .build();

    }

    @Override
    public UserModel getUserDetailFromToken(String token) throws Exception {
        if (jwtTokenUtil.isTokenExpired(token)){
            throw new Exception("Token is expired") ;
        }
        String phone = jwtTokenUtil.extractPhone(token) ;
        Optional<UserModel> userModel = userRepository.findByPhone(phone) ;

        if (userModel.isPresent()){
            return userModel.get() ;
        }else {
            throw new Exception("User not found") ;
        }
    }

    @Override
    public UserModel updateUserByAdmin(int id, UserDTOUpdateByAdmin userDTOUpdate) throws DataNotFoundException, UnauthorizedException {
        // Lấy số điện thoại hoặc tên của người dùng hiện tại từ SecurityContext
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserModel currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + currentPhone));

        // Lấy thông tin người dùng cần cập nhật
        UserModel existingUser = getUserById(id);

        // Cập nhật các thông tin cơ bản
        existingUser.setPhone(userDTOUpdate.getPhone());
        existingUser.setName(userDTOUpdate.getName());
        existingUser.setEmail(userDTOUpdate.getEmail());
        existingUser.setGender(userDTOUpdate.getGender());
        existingUser.setAddress(userDTOUpdate.getAddress());

        // Xử lý roleId: Chỉ admin mới có thể thay đổi
        if (userDTOUpdate.getRoleId() != null) {
            if (!currentUser.getRoleId().getRoleName().equals("ADMIN")) {
                throw new UnauthorizedException("Bạn không có quyền cập nhật thông tin người dùng khác");
            }
            RoleModel roleModel = roleRepository.findById(userDTOUpdate.getRoleId())
                    .orElseThrow(() -> new DataNotFoundException("Role not found"));
            existingUser.setRoleId(roleModel);
        }

        // Nếu roleId không được truyền, giữ nguyên giá trị hiện tại
        userRepository.save(existingUser);

        return existingUser;
    }






    @Override
    public UserModel getUserById(int id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User with id = " + id + " not found"));
    }

    @Override
    public Page<UserResponse> getUserResponse(PageRequest pageRequest) {
        return userRepository.findAll(pageRequest).map(UserResponse::fromUser);
    }

    @Override
    public void  deleteUser(int id) {
        UserModel user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        userRepository.delete(user);
    }

    @Override
    public UserModel updateUser(int id, UserDTOUpdate userDTOUpdate) {
        UserModel existingUser = getUserById(id);
        existingUser.setPhone(userDTOUpdate.getPhone());
        existingUser.setName(userDTOUpdate.getName());
        existingUser.setEmail(userDTOUpdate.getEmail());
        existingUser.setGender(userDTOUpdate.getGender());
        existingUser.setAddress(userDTOUpdate.getAddress());
        userRepository.save(existingUser) ;
        return existingUser;
    }


}

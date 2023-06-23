package jp.ebacorp.Hiroyuki.Nishi.EBAcorpPMS;

import jp.ebacorp.Hiroyuki.Nishi.EBAcorpPMS.task.storage.AttachFile;
import jp.ebacorp.Hiroyuki.Nishi.EBAcorpPMS.task.storage.CRUDAttachFileRepository;
import jp.ebacorp.Hiroyuki.Nishi.EBAcorpPMS.task.storage.FileSystemStorageService;
import jp.ebacorp.Hiroyuki.Nishi.EBAcorpPMS.task.CRUDTaskFormRepository;
import jp.ebacorp.Hiroyuki.Nishi.EBAcorpPMS.task.TaskForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class MainController {

    private final FileSystemStorageService storageService;

    @Autowired
    public MainController(FileSystemStorageService storageService) {
        this.storageService = storageService;
    }

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CRUDTaskFormRepository TaskFormRepository;

    @Autowired
    CRUDAttachFileRepository attachFileRepository;

    @Autowired
    DataSource dataSource;
    @GetMapping
    String getIndex(Model model){
        List<TaskForm> TaskList = (List<TaskForm>) TaskFormRepository.findAll();

        model.addAttribute("taskTest",TaskList);
        model.addAttribute("JSFS", "JavascriptFromSpring");
        return "index";
    }

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/signup")
    public String getSignUp(SignupForm signupForm) {
        return "signup";
    }

    @PostMapping("/signup")
    public String postSignUp(@Validated SignupForm signupForm,
                         BindingResult bindingResult,
                         Model model) {
        if (!bindingResult.hasErrors()) {
            JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
            if (users.userExists(signupForm.getUsername())) {
                System.out.println("Already exist");
                model.addAttribute("signupError", "ユーザー名 " + signupForm.getUsername() + "は既に登録されています");
            } else {
                UserDetails newUser = User.builder()
                        .username(signupForm.getUsername())
                        .password(passwordEncoder.encode(signupForm.getPassword1()))
                        .roles("USER")
                        .build();
                try {
                    users.createUser(newUser);
                    model.addAttribute("message", "ユーザー登録に成功しました。");
                    return "login";
                } catch (DataAccessException e) {
                    model.addAttribute("signupError", "ユーザー登録に失敗しました。");
                }
            }
        }
        return "signup";
    }

    @GetMapping("/newtask")
    public String getNewTask(TaskForm taskForm,
                             AttachFile attachFile){
        return "ticketdetail";
    }

    @PostMapping("/newtask")
    public String postNewTask(@Validated TaskForm taskForm,
                           BindingResult bindingResult,
                           Model model){
        if (!bindingResult.hasErrors()) {
            try{

                TaskFormRepository.save(taskForm);
                model.addAttribute("message", "タスク登録に成功しました");
                }catch(Exception e){
                model.addAttribute("message", "タスク登録に失敗しました");
            }
        }
        return "ticketdetail";
    }

    @GetMapping("/ticket/{id}")
    public String getTaskDetail(@PathVariable Integer id,
                                 TaskForm taskForm,
                                 Model model,
                                AttachFile attachFile){

        Optional<TaskForm> taskFormFromDB = TaskFormRepository.findById(id);
        List<AttachFile> attachFiles = attachFileRepository.findByTicketidEquals(id);

        taskForm = taskFormFromDB.get();

        model.addAttribute("taskForm", taskForm);
        model.addAttribute("attachFile", attachFiles);

        return "ticketdetail";
    }
    @PostMapping("/ticket/{id}")
    public String postTaskDetail(@PathVariable Integer id,
                                 @Validated TaskForm taskForm,
                                 BindingResult bindingResult,
                                 Model model){
        if (!bindingResult.hasErrors()) {
            try{
                TaskFormRepository.save(taskForm);
                model.addAttribute("message", "タスク更新に成功しました");
            }catch(Exception e){
                model.addAttribute("message", "タスク更新に失敗しました");
            }
        }
        return "ticketdetail";


    }

    @PostMapping("/fileupload/{id}")
    public String postFileUpload(@PathVariable Integer id,
                                 @RequestParam("attachFile") MultipartFile file,
                                 RedirectAttributes redirectAttributes){
        storageService.store(file, id);

        redirectAttributes.addFlashAttribute("message",
                "ファイル " + file.getOriginalFilename() + "のアップロードが完了しました。");
        return "redirect:/ticket/" + id;
    }

    @GetMapping("/file/{id}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename,
                                              @PathVariable Integer id) {

        Resource file = storageService.loadAsResource(filename, id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }
}

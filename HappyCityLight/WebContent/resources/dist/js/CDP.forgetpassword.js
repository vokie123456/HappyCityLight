function resetPass() {
    var email = $("#email").val();
    $
        .ajax({
            type: "POST",
            crossDomain: true,
            url: serverURL + "user/sendresetpwdmail",
            data: {
                "email": email
            },
            beforeSend: function () {
                //alert("click on submit!");
                $("title").html("LOADING ...");
                $(".input-group-btn").addClass("overlay");
                $("#arrow").removeClass("fa-arrow-right");
                $("#arrow").addClass("fa-refresh fa-spin");
                $("#arrow").attr("disabled", "true");
            },
            success: function (data) {
                //                        alert(JSON.stringify(data));
                if (data.code == 200) {
                    window.location.href = "login.html?message=" + data.message;
                } else {
                    $("title").html(
                        "City Digital Pulse | Forget password");
                    $(".input-group-btn").removeClass("overlay");
                    $("#arrow").addClass("fa-arrow-right");
                    $("#arrow").removeClass("fa-refresh fa-spin");
                    $("#arrow").attr("disabled", "false");
                    //window.location.href = "error.html?tye=email";
                }
            },
            error: function (data) {
                $("title").html(
                    "City Digital Pulse | Forget password");
                $(".input-group-btn").removeClass("overlay");
                $("#arrow").addClass("fa-arrow-right");
                $("#arrow").removeClass("fa-refresh fa-spin");
                $("#arrow").attr("disabled", "false");
                alert(data.responseJSON.error);
                if (data.responseJSON.error.indexOf("MailError") > -1) {
                    //window.location.href = "<c:url value="/emailError.html"></c:url>";
                    $("#mailerror").css("display", "block");
                } else {
                    window.location.href = "login.html";
                }
            }

        });
}
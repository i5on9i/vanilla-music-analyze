set file="ic_drawer_gun.png"

@rem set frompath="d:\mine\programming\androidStudio\vanilla\res"
set frompath="d:\mine\programming\androidStudio\gtatube\app\src\main\res"

set topath="d:\mine\programming\androidStudio\jidae\app\src\main\res"
copy %frompath%\drawable-hdpi\%file% %topath%\drawable-hdpi
copy %frompath%\drawable-mdpi\%file% %topath%\drawable-mdpi
copy %frompath%\drawable-xhdpi\%file% %topath%\drawable-xhdpi
copy %frompath%\drawable-xxhdpi\%file% %topath%\drawable-xxhdpi
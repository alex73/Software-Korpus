jQuery(document).ready(function($) {
  
  $(document).on("click", ".spell_switcher a", function(e){
    e.preventDefault();
    var clickedIndex = $(e.target).closest("li").index();
    var spellSwitcher = $(e.target).closest(".spell_switcher");
    if(clickedIndex>0){
      spellSwitcher.removeClass("first_active");
      spellSwitcher.addClass("second_active");
      spellSwitcher.find("li.active").removeClass("active");
      spellSwitcher.find("li").eq(clickedIndex).addClass("active");
      switchClassic();
    }
    else {
      spellSwitcher.addClass("first_active");
      spellSwitcher.removeClass("second_active");
      spellSwitcher.find("li.active").removeClass("active");
      spellSwitcher.find("li").eq(clickedIndex).addClass("active");
      switchOfficial();
    }

  });


  // Switch classic spelling
  function switchOfficial(){
    console.log("Official spelling");
  }

  // Switch classic spelling
  function switchClassic(){
    console.log("Classic spelling");
  }




});